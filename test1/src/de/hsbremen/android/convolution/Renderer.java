package de.hsbremen.android.convolution;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Exchanger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import de.hsbremen.android.convolution.buffer.NativeBuffers;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

public abstract class Renderer<T extends Processor> {
	private static class Multiple<MT> {
		public final Lock unusedLock = new ReentrantLock();
		public MT unused;
		public final List<MT> queued  = Collections.synchronizedList( new ArrayList<MT>() );
		
		public Multiple() {
			queued.add( null );
		}
	}
	
	private final T                _processor;
	private final ProgressListener _progress;
	private final RenderListener   _renderListener;
	private final Handler          _renderHandler;
	
	public final CameraProcessor.Listener listener = new CameraProcessor.Listener() {
		private final Multiple<ByteBuffer> _buffers = new Multiple<ByteBuffer>();
		private final Multiple<int[]>      _kernels = new Multiple<int[]>();
		
		private <MT> MT exchangeQueued( Multiple<MT> multiple, MT buffer ) {
			return multiple.queued.set( 0, buffer );
		}
		
		private <MT> MT getQueued( Multiple<MT> multiple ) {
			return multiple.queued.get( 0 );
		}
		
		private void setUnused( Multiple<ByteBuffer> collection, ByteBuffer element ) {
			collection.unusedLock.lock();
			try {
				if( collection.unused == null || collection.unused.capacity() < element.capacity() ) {
					collection.unused = element;
				}
			} finally {
				collection.unusedLock.unlock();
			}
		}
		
		private void setUnused( Multiple<int[]> collection, int[] element ) {
			collection.unusedLock.lock();
			try {
				if( collection.unused == null || collection.unused.length < element.length ) {
					collection.unused = element;
				}
			} finally {
				collection.unusedLock.unlock();
			}
		}
		
		@Override
		public void onPreviewFrameProcess( final int width, final int height ) {
			executeInRenderThread( new Runnable() {
				@Override
				public void run() {
					final int[]      kernel = getQueued( _kernels );
					final ByteBuffer buffer = exchangeQueued( _buffers, null );
					
					if( buffer == null )
						return; // Nothing to process
					
					getProcessor().convolute( buffer, width, height, kernel, _progress, _renderListener );
					setUnused( _buffers, buffer );
					setUnused( _kernels, kernel );
				}
			} );
		}
		
		@Override
		public void onPreviewFrameCopy( byte[] data ) {
			_buffers.unusedLock.lock();
			try {
				ByteBuffer buffer = _buffers.unused;
		
				if( buffer == null ) {
					Log.v( getClass().getSimpleName(), "Allocated new native ByteBuffer with size " + data.length);
				} else if( buffer.capacity() < data.length ) {
					Log.v( getClass().getSimpleName(), "Reallocated new native ByteBuffer with size " + data.length + " (old " + buffer.capacity() + ")" );
					buffer = null;
				}
						
				if( buffer == null ) {
					// Allocate backbuffer
					buffer = NativeBuffers.allocateByte( data.length );
					_buffers.unused = buffer; // Make sure we always have one spare
					// Allocate frontbuffer
					buffer = NativeBuffers.allocateByte( data.length );
				}
					
				buffer.rewind();
				buffer.put( data );
				
				buffer = exchangeQueued( _buffers, buffer );
				if( buffer != null )
					_buffers.unused = buffer;
			} finally {
				_buffers.unusedLock.unlock();
			}
		}
		
		@Override
		public void onKernelChange(int[] newKernel) {
			_kernels.unusedLock.lock();
			try {
				int[] kernel = _kernels.unused;
				
				if( kernel == null || kernel.length < newKernel.length ) {
					Log.v( getClass().getSimpleName(), "Allocating new kernel buffer");
					kernel = newKernel.clone();
					_kernels.unused = kernel;
					kernel = newKernel.clone();
				} else
					System.arraycopy( newKernel, 0, kernel, 0, newKernel.length );
		
				kernel = exchangeQueued( _kernels, kernel );
				
				if( kernel != null )
					_kernels.unused = kernel;
			} finally {
				_kernels.unusedLock.unlock();
			}
		}
	};
	
	public Renderer( T processor, ProgressListener progress, RenderListener renderListener ) {
		_processor = processor;
		_progress = progress;
		_renderListener = renderListener;
		_renderHandler = createHandler(); 
	}
	
	private static Handler createHandler() {
		// Use Exchanger so transfer between threads is possible
		final Exchanger<Handler> exchanger = new Exchanger<Handler>();
		// Process in own thread so main thread is not blocked
		final Thread renderThread = new Thread() {
			@Override
			public void run() {
				super.run();
				
				Looper.prepare();
				exchange( exchanger, new Handler() );
				Looper.loop();
			}
		};
		renderThread.start();
		return exchange( exchanger, null );
	}
	
	// Method necessary because syntax analysis is weak
	private static <ET> ET exchange( Exchanger<ET> exchanger, ET obj ) {
		while( true ) {
			try {
				return exchanger.exchange( obj );
			} catch( InterruptedException e ) {
			}
		}
	}
	
	protected T getProcessor() {
		return _processor;
	}
	
	private void executeInRenderThread( Runnable runnable ) {
		_renderHandler.post( runnable );
	}
	
	public void close() {
		final Exchanger<Thread> exchanger = new Exchanger<Thread>();
		executeInRenderThread( new Runnable() {
			@Override
			public void run() {
				exchange( exchanger, Thread.currentThread() );
				Looper looper = Looper.myLooper();
				if( looper != null ) {
					looper.quit();
				}
			}
		} );
		// Retrieve renderThread again
		Thread renderThread = exchange( exchanger, null );
		while( true ) {
			// Make sure close waits till other thread is closed
			try {
				renderThread.join();
				break;
			} catch( InterruptedException e ) {
			}
		}
	}
}
