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
	typedef std::uint32_t           rgb_type;
	typedef std::vector<rgb_type>   buffer_type;
	typedef std::uint8_t            color_type;
	typedef std::vector<color_type> color_buffer_type;

	template <short SHIFT>
	constexpr color_type color( rgb_type color ) noexcept {
		return (color >> SHIFT) & 0xFF;
	}

	struct SHIFT {
		static const short RED   = 16;
		static const short GREEN =  8;
		static const short BLUE  =  0;
	};

	constexpr color_type red  (rgb_type rgb) noexcept { return color<SHIFT::RED  >(rgb) & 0xFF; }
	constexpr color_type green(rgb_type rgb) noexcept { return color<SHIFT::GREEN>(rgb) & 0xFF; }
	constexpr color_type blue (rgb_type rgb) noexcept { return color<SHIFT::BLUE >(rgb) & 0xFF; }

	template <short SHIFT>
	constexpr void merge_value(rgb_type& rgb, color_type color) noexcept {
		rgb |= (color & 0xFF) << SHIFT;
	}

	template <short SHIFT>
	void mergeFunc( buffer_type& collection, color_buffer_type::const_iterator it ) noexcept {
		for( auto& value : collection ){
			merge_value<SHIFT>(value, *it);
			++it;
		}
	}

	constexpr size_t offset(size_t x, size_t y, size_t width) noexcept {
		return y * width + x;
	}

	template <short SHIFT>
	color_type channel_pixel( const buffer_type& input, size_t x, size_t y, size_t width ) noexcept {
		return color<SHIFT>( input[offset(x, y, width)] );
	}

	template <short SHIFT>
	std::vector<color_type> colorFunc( const buffer_type& input, size_t width, size_t height, int const * kernel ) noexcept {
		std::vector<color_type> output( width * height );

		for( size_t y = 0; y < height; ++y ) {
			for( size_t x = 0; x < width; ++x ) {

				// This is int value for performance
				unsigned int value;
				for( std::int8_t dx = -2; dx != 3; ++dx ) {
					for( std::int8_t dy = -2; dy != 3; ++dy ) {
						const auto kern = kernel[(2 + dx) + (2 + dy) * 5];
						// Save old value to enable under/overflow detection
						auto old = value;
						value += channel_pixel<SHIFT>( input, x + dx, y + dy, width ) * kern;

						// Handle underflow
						if( kern < 0 && value > old )
							value = std::numeric_limits<int>::max();

						// Handle overflow
						if( kern > 0 && value < old )
							value = std::numeric_limits<int>::min();
					}
				}

				output[offset(x, y, width)] = value;
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

		return output;
	}

	typedef std::function<color_buffer_type()> process_function;
	typedef std::function<void(color_buffer_type::const_iterator)>    merge_function;
	typedef std::tuple<process_function, merge_function> function_tuple;

	template <short SHIFT>
	function_tuple make_tuple( buffer_type& buffer, size_t width, size_t height, int const * kernel ) noexcept {
		process_function proc  = std::bind( &colorFunc<SHIFT>, std::ref(buffer), width, height, kernel );

		merge_function merge = std::bind( &mergeFunc<SHIFT>, std::ref(buffer), std::placeholders::_1 );
		return function_tuple( proc, merge );
	}
	void exec( const std::array<function_tuple, 3> function_tuples,
		           size_t width,
		           size_t height ) __attribute__ ((pure));
	// Use pure function to help optimizer
	void exec( const std::array<function_tuple, 3> function_tuples,
	           size_t width,
	           size_t height ) {
		buffer_type output( width * height, 0xFF000000 );

		{
			// Try to move all resulting output to temporary
			// array so we remove data dependency and
			// enable vectorizing
			std::array<color_buffer_type, 3> outputs;

			{
				auto outputIt = outputs.begin();

				// Data should have no dependency
				for( auto tuple : function_tuples ) {
					// Apply serialized so no locking necessary

					process_function function;
					std::tie( function, std::ignore ) = tuple;

					// Get processed channel
					*outputIt = function();
				}
			}

			{
				auto outputIt = outputs.begin();

				for( auto tuple : function_tuples ) {
					merge_function merger;
					std::tie( std::ignore, merger ) = tuple;

					merger( outputIt->begin() );
				}
				// After last merge result should be sufficient
			}
		}
	}

	const buffer_type& convolute( buffer_type&  buffer, size_t width, size_t height, int32_t const* kernel ) noexcept {

		auto function_tuples = [&]() -> std::array<function_tuple, 3> {
			std::array<function_tuple, 3> temp{{
				make_tuple<SHIFT::RED  >(buffer, width, height, kernel),
				make_tuple<SHIFT::GREEN>(buffer, width, height, kernel),
				make_tuple<SHIFT::BLUE >(buffer, width, height, kernel) }};
			return temp;
		}();

		exec( function_tuples, width, height );
	}

	template <typename T>
	inline T clamp(T value, T min, T max) {
		return std::min(std::max(value, min), max);
	}

	template <typename T>
	inline T clamp8( T value ) {
		return clamp( value, 0, 255 );
	}

	inline void convertYUVtoRGB( std::uint8_t y, std::uint8_t u, std::uint8_t v, buffer_type& rgb, std::size_t index ) {
		const auto rs = y + (unsigned short)(1.402f * v);
		const auto gs = y - (unsigned short)(0.344f * u + 0.714f * v);
		const auto bs = y + (unsigned short)(1.772f * u);
		// Normalize values

		rgb[ index + 0 ] = clamp8( rs );
		rgb[ index + 1 ] = clamp8( gs );
		rgb[ index + 2 ] = clamp8( bs );
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
			u = u-128;
			v = v-128;

			convertYUVtoRGB( y1, u, v, rgb, i + 0         );
			convertYUVtoRGB( y2, u, v, rgb, i + 1         );
			convertYUVtoRGB( y3, u, v, rgb, i + width + 0 );
			convertYUVtoRGB( y4, u, v, rgb, i + width + 1 );

			if (i != 0 && (i + 2) % width == 0 ) {
				i += width;
				increaseProgress( width * 2 );
			}
		}
	}

/*
	// Compile time processing
	template <unsigned int N>
	inline unsigned int fib_4() {
		if( N == 0 )
			return 0;
		if( N == 1 )
			return 1;
		return fib_4<N-1>() + fib_4<N-2>();
	}
*/
	std::clock_t now() {
		return std::clock() / (CLOCKS_PER_SEC / 1000);
	}

	long duration( std::clock_t t0, std::clock_t t1 ) {
		return t1 - t0;
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
}

/*
extern "C" JNIEXPORT
void JNICALL Java_de_hsbremen_android_convolution_nio_Processor_Process(JNIEnv * pEnv, jclass, jobject frame, jint width, jint height, jobject kernelObject) {
	auto frameBuffer = static_cast<std::int8_t*>( pEnv->GetDirectBufferAddress(frame) );
	auto kernel = static_cast<int32_t*>( pEnv->GetDirectBufferAddress(kernelObject) );

	// Make sure we have enough space
	static buffer_type BUFFER;

	convertYUV420_NV21toRGB888( frameBuffer, width, height, BUFFER );

	convolute( BUFFER, width, height, kernel );

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
