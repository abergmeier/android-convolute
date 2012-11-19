
#include <jni.h>
#include <android/log.h>
#include <ctime>
#include <algorithm>
#include <GLES2/gl2.h>
#include <cpu-features.h>
#include <cstdint>
#include <vector>
#include <limits>
#include <boost/thread/thread.hpp>
#include <boost/thread/future.hpp>

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
	constexpr color_type color( rgb_type color ) {
		return (color >> SHIFT) & 0xFF;
	}

	struct SHIFT {
		static const short RED   = 16;
		static const short GREEN =  8;
		static const short BLUE  =  0;
	};

	constexpr color_type red  (rgb_type rgb) { return color<SHIFT::RED  >(rgb) & 0xFF; }
	constexpr color_type green(rgb_type rgb) { return color<SHIFT::GREEN>(rgb) & 0xFF; }
	constexpr color_type blue (rgb_type rgb) { return color<SHIFT::BLUE >(rgb) & 0xFF; }

	template <short SHIFT>
	constexpr void merge(rgb_type& rgb, color_type color) {
		rgb |= (color & 0xFF) << SHIFT;
	}

	template <short SHIFT>
	void merge( buffer_type::reference collection, color_buffer_type::const_iterator it ) {
		for( auto& value : collection ){
			merge<SHIFT>(value, *it);
			++it;
		}
	}

	size_t offset(size_t x, size_t y, size_t width) {
		return y * width + x;
	}

	template <short SHIFT>
	color_type channel_pixel( const buffer_type& input, size_t x, size_t y, size_t width ) {
		return color<SHIFT>( input[offset(x, y, width)] );
	}

	template <short SHIFT>
	std::vector<color_type> colorFunc( const buffer_type& input, size_t width, size_t height, int const * kernel ) {
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
						value += channel_pixel<SHIFT>( x + dx, y + dy ) * kern;

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

	typedef boost::shared_future<color_buffer_type> process_function;

	template <typename FT>
	boost::packaged_task<color_buffer_type> start(FT function, const buffer_type& buffer, size_t width, size_t height, int* kernel ) {
		boost::packaged_task<color_buffer_type> task( boost::bind( function, std::ref(buffer), width, height, kernel ) );
		return task;
	}

	typedef std::function<void(color_buffer_type::const_iterator)>                        merge_function;
	typedef std::tuple<process_function, merge_function> function_tuple;

	template <short SHIFT>
	function_tuple make_tuple( buffer_type& buffer, size_t width, size_t height, int const * kernel ) {
		auto proc  = start( colorFunc<SHIFT>, std::ref(buffer), width, height, kernel );
		auto merge = std::bind( merge<SHIFT>, _1 );
		return function_tuple( proc.get_future(), merge );
	}

	void proc( size_t width, size_t height, int const* kernel ) {
		// Make sure we have enough space
		static buffer_type BUFFER;
		BUFFER.reserve( width * height );
		glReadPixels( 0, 0, width, height, GL_RGB, GL_UNSIGNED_INT, BUFFER.data() );

		auto function_tuples = [&]() -> std::array<function_tuple> {
			std::array<function_tuple, 3> temp{{
				make_tuple<SHIFT::RED  >(BUFFER, width, height, kernel),
				make_tuple<SHIFT::GREEN>(BUFFER, width, height, kernel),
				make_tuple<SHIFT::BLUE >(BUFFER, width, height, kernel) }};
			return temp;
		}();

		buffer_type output( width * height, 0xFF000000 );

		// Wait on all processing to commence
		for( auto future : function_tuples ) {
			// Apply serialized so no locking necessary

			process_function future;
			merge_function   merger;
			std::tie( future, merger ) = future;

			// Get processed channel
			auto colorOutput = future.get();
			merger( output, colorOutput.begin() );
		}

		// After last merge result should be sufficient
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

	const char* LOG_TAG = "FIB_TEST";
}

#define LOGI(...) ((void)__android_log_print(ANDROID_LOG_INFO   , LOG_TAG, __VA_ARGS__))

extern "C" JNIEXPORT void JNICALL Java_com_example_test1_NativeConvolution_nativeConvolute(JNIEnv *, jclass, jint texName, jarray kernel) {
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
