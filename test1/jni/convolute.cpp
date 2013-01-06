#include <jni.h>
#include <android/log.h>
#include <ctime>
#include <algorithm>
#include <GLES2/gl2.h>
#include <GLES2/gl2ext.h>
#include <cstdint>
#include <vector>
#include <array>
#include <limits>
#include <functional>

namespace std {
	typedef ::int8_t   int8_t;
	typedef ::int16_t  int16_t;
	typedef ::int32_t  int32_t;
	typedef ::uint8_t  uint8_t ;
	typedef ::uint16_t uint16_t;
	typedef ::uint32_t uint32_t;
}

namespace {
	typedef std::uint8_t            color_type;
	typedef std::vector<color_type> buffer_type;

	struct SHIFT {
		static const short RED   = 0;
		static const short GREEN = 1;
		static const short BLUE  = 2;
	};

	constexpr std::size_t offset( std::size_t x, std::size_t y, std::size_t width, std::uint8_t bytesPerPixel ) noexcept {
		return (y * width + x) * bytesPerPixel;
	}

	template <short SHIFT>
	color_type channel_pixel( const buffer_type& input,
	                          std::size_t x, std::size_t y,
	                          std::size_t width, std::uint8_t bytesPerPixel ) noexcept {
		return input[offset(x, y, width, bytesPerPixel) + SHIFT];
	}

	template <short SHIFT>
	void colorFunc( const buffer_type& input, size_t width, size_t height, std::uint8_t bytesPerPixel, int const * kernel, std::uint8_t* output ) noexcept {

		for( size_t y = 0; y < height; ++y ) {
			for( size_t x = 0; x < width; ++x ) {

				// This is int value for performance
				unsigned int value;
				for( std::int8_t dx = -2; dx != 3; ++dx ) {
					for( std::int8_t dy = -2; dy != 3; ++dy ) {
						const auto kern = kernel[(2 + dx) + (2 + dy) * 5];
						// Save old value to enable under/overflow detection
						auto old = value;
						value += channel_pixel<SHIFT>( input, x + dx, y + dy, width, bytesPerPixel ) * kern;

						// Handle underflow
						if( kern < 0 && value > old )
							value = std::numeric_limits<int>::max();

						// Handle overflow
						if( kern > 0 && value < old )
							value = std::numeric_limits<int>::min();
					}
				}

				output[offset(x, y, width, bytesPerPixel) + SHIFT] = value;
			}
		}
	/*
			for( size_t y = 0; y < height; ++y ) {
				for( size_t x = 0; x < width; ++x ) {

					int r = 0,
					    g = 0,
					    b = 0;

					for( int dx = -2; dx != 3; ++dx ) {
						for( int dy = -2; dy != 3; ++dy ) {
							final int kern = kernel[(2 + dx) + (2 + dy) * 5];
							int pixel = getPixel( x + dx, y + dy );
							r += red  (pixel) * kern;
							g += green(pixel) * kern;
							b += blue (pixel) * kern;
						}
					}

					setPixel( destPixels, x, y, rgb(r, g, b));
				}
			}
			*/
	}

	void convolute( const buffer_type&  buffer, std::size_t width, std::size_t height, std::uint8_t bytesPerPixel, int32_t const* kernel, std::uint8_t* output ) noexcept {
		colorFunc<SHIFT::RED  >( buffer, width, height, bytesPerPixel, kernel, output );
		colorFunc<SHIFT::GREEN>( buffer, width, height, bytesPerPixel, kernel, output );
		colorFunc<SHIFT::BLUE >( buffer, width, height, bytesPerPixel, kernel, output );
	}

	template <typename T>
	inline T clamp(T value, T min, T max) {
		return std::min(std::max(value, min), max);
	}

	template <typename T>
	inline T clamp_u8( T value ) {
		return clamp( value, 0, 255 );
	}

	inline void convertYUVtoRGB( std::uint8_t y, std::int8_t u, std::int8_t v, std::uint8_t bytesPerPixel, buffer_type& rgb, std::size_t index ) {
		const auto r = y + static_cast<std::int16_t>(1.402f * v);
		const auto g = y - static_cast<std::int16_t>(0.344f * u + 0.714f * v);
		const auto b = y + static_cast<std::int16_t>(1.772f * u);
		// Normalize values

		index = index * bytesPerPixel;
		rgb[ index + 0 ] = clamp_u8( r );
		rgb[ index + 1 ] = clamp_u8( g );
		rgb[ index + 2 ] = clamp_u8( b );
	}

	jmethodID getMethod( JNIEnv * pEnv, jobject progress, const char* name, const char* signature ) {
		auto clazz = pEnv->GetObjectClass( progress );
		return pEnv->GetMethodID( clazz, name, signature );
	}

	jmethodID incrementMethod( JNIEnv * pEnv, jobject progress ) {
		static auto METHOD_ID = getMethod( pEnv, progress, "incrementBy", "(I)V" );
		return METHOD_ID;
	}

	jmethodID getMaxMethod( JNIEnv * pEnv, jobject progress ) {
		static auto METHOD_ID = getMethod( pEnv, progress, "getMax" , "()I" );
		return METHOD_ID;
	}

	jmethodID setMaxMethod( JNIEnv * pEnv, jobject progress ) {
		static auto METHOD_ID = getMethod( pEnv, progress, "setMax" , "(I)V" );
		return METHOD_ID;
	};

	void convertYUV420_NV21toRGB888( std::uint8_t* data,
	                                 std::size_t width, std::size_t height, std::uint8_t bytesPerPixel,
	                                 buffer_type& rgb, std::function<void(jint)>& increaseProgress ) {
		const auto size = width * height;
		const auto offset = size;

		rgb.resize( size * bytesPerPixel );

		// i along Y and the final pixels
		// k along pixels U and V
		for( size_t i = 0, k = 0; i < size; i += 2, k += 2) {
			const auto y1 = data[i + 0];
			const auto y2 = data[i + 1];
			const auto y3 = data[i + width + 0];
			const auto y4 = data[i + width + 1];

			auto u = data[k + offset + 0];
			auto v = data[k + offset + 1];
			u = u - 128;
			v = v - 128;

			convertYUVtoRGB( y1, u, v, bytesPerPixel, rgb, i + 0         );
			convertYUVtoRGB( y2, u, v, bytesPerPixel, rgb, i + 1         );
			convertYUVtoRGB( y3, u, v, bytesPerPixel, rgb, i + width + 0 );
			convertYUVtoRGB( y4, u, v, bytesPerPixel, rgb, i + width + 1 );

			if(i != 0 && (i + 2) % width == 0 ) {
				i += width;
				increaseProgress( width * 2 );
			}
		}
	}

	const char* LOG_TAG = "CONVOLUTE";
}

#define LOGI(...) ((void)__android_log_print(ANDROID_LOG_INFO   , LOG_TAG, __VA_ARGS__))
#define LOGE(...) ((void)__android_log_print(ANDROID_LOG_ERROR  , LOG_TAG, __VA_ARGS__))

extern "C" JNIEXPORT void JNICALL
Java_de_hsbremen_android_convolution_nio_Processor_nativeProcess( JNIEnv * pEnv, jclass, jobject frameBuffer, jint width, jint height, jbyte bytesPerPixel,
                                                                  jobject kernelObject, jobject outputBuffer, jobject progress ) {
	auto frame  = static_cast<std::uint8_t*>( pEnv->GetDirectBufferAddress(frameBuffer ) );
	auto kernel = static_cast<int32_t*     >( pEnv->GetDirectBufferAddress(kernelObject) );
	auto output = static_cast<std::uint8_t*>( pEnv->GetDirectBufferAddress(outputBuffer) );

	// Make sure we have enough space
	static buffer_type BUFFER;

	{
		const auto size = width * height;
		auto methodId = setMaxMethod( pEnv, progress );
		pEnv->CallVoidMethod( progress, methodId, size * 2);
	}
	{
		std::function<void(jint)> increaseProgress = [&]( jint increase ) {
			auto methodId = incrementMethod( pEnv, progress );
			pEnv->CallVoidMethod( progress, methodId, increase );
		};
		convertYUV420_NV21toRGB888( frame, width, height, bytesPerPixel, BUFFER, increaseProgress );
	}

	{
		auto destSize = pEnv->GetDirectBufferCapacity( outputBuffer );
		if( destSize < BUFFER.size() ) {
			LOGE( "Cannot copy %u to output of size %d.", BUFFER.size(), destSize );
			return;
		}
	}

	std::copy( BUFFER.begin(), BUFFER.end(), output );
	//convolute( BUFFER, width, height, kernel, bytePerPixel, output );
}

/*
extern "C" JNIEXPORT
void JNICALL Java_de_hsbremen_android_convolution_nio_Processor_Process(JNIEnv * pEnv, jclass, jobject frame, jint width, jint height, jobject kernelObject) {
	auto frameBuffer = static_cast<std::int8_t*>( pEnv->GetDirectBufferAddress(frame) );
	auto kernel = static_cast<int32_t*>( pEnv->GetDirectBufferAddress(kernelObject) );

	// Make sure we have enough space
	static buffer_type BUFFER;

	convertYUV420_NV21toRGB888( frameBuffer, width, height, BUFFER );

	convolute( BUFFER, width, height, kernel, bytePerPixel );

	// Copy data to GL_TEXTURE_EXTERNAL_OES storage
	glTexImage2D( GL_TEXTURE_EXTERNAL_OES,
	              0,
	              GL_RGB,
	              width,
	              height,
	              0,
	              GL_RGB,
	              GL_UNSIGNED_INT,
	              BUFFER.data() );
/*

	// Load the vertex data
	GLES20.glVertexAttribPointer( _vertexPosition, 3, GLES20.GL_FLOAT,
	                              false, 0, mCube.getVertices() );
	GLES20.glEnableVertexAttribArray( _vertexPosition );

	// Load the MVP matrix
	GLES20.glUniformMatrix4fv( mMVPLoc, 1, false,
	                           mMVPMatrix.getAsFloatBuffer() );

	// Draw the cube
	GLES20.glDrawElements( GLES20.GL_TRIANGLES, mCube.getNumIndices(),
	                       GLES20.GL_UNSIGNED_SHORT, mCube.getIndices());


#if 0
	{
		auto t0 = now();
		fib_1( count );
		LOGI( "Native Time: (noopt) %ul", duration(t0, now()) );
	}

	{
		auto t0 = now();
		fib_3( count );
		LOGI( "Native Time: (inline) %ul", duration(t0, now()) );
	}
#endif
}
*/
