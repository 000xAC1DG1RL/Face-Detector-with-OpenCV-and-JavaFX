package application;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.videoio.VideoCapture;

import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.text.Text;

public class FXController {
	// the FXML button
	@FXML
	private Button button;
	// the FXML image view
	@FXML
	private ImageView currentFrame;

	@FXML
	private Text fps;

	@FXML
	private Pane ImageViewPane;

	// the OpenCV object that realizes the video capture
	private VideoCapture capture = new VideoCapture(0);
	// a flag to change the button behavior
	private static boolean cameraActive = false;

	// Settings

	// Set video device
	
	private CascadeClassifier face_cascade;
    private long detectedFaces;
    public BufferedImage croppedImage;

	public void initController(int vd) {
		
		face_cascade = new CascadeClassifier("C:/Users/Admin/Documents/opencv/sources/data/haarcascades/haarcascade_frontalface_alt.xml");  
        if(face_cascade.empty()){  
             System.out.println("--(!)Error loading A\n");  
             return;  
        }  
        else{  
             System.out.println("Face classifier loooaaaaaded up");  
        }  
	}

	/**
	 * The action triggered by pushing the button on the GUI
	 *
	 * @param event
	 *            the push button event
	 */
	@FXML
	protected void startCamera(ActionEvent event) {
		try {
			if (!cameraActive) {
				// start the video capture
				//this.capture.open(videodevice);

				// is the video stream available?
				if (this.capture.isOpened()) {
					cameraActive = true;
					Thread processFrame = new Thread(new Runnable() {

						@Override
						public void run() {
							while (cameraActive) {
								try {
									//Grab Frame
									Mat matToShow = grabFrame();
									//Process Frame
									matToShow = processMat(detect(matToShow));
									// convert the Mat object (OpenCV) to Image (JavaFX)
									Image imageToShow = mat2Image(matToShow);
									//Update ImageView
									setFrametoImageView(imageToShow);
									//Update the UI
									updateUIObjects();

								} catch (Exception e1) {
									System.out.println("Error on Update " + e1);
								}
								// Sleep for lower FPS/updateRate
								// try {
								// Thread.sleep(10);
								// } catch (InterruptedException e) {
								// // TODO Auto-generated catch block
								// e.printStackTrace();
								// }

							}
							System.out.println("Thread processFrame closed");
							try {
								capture.release();
								updateUIObjects();
								setFrametoImageView(null);
							} catch (Exception e) {
							}

						}

					});
					processFrame.setDaemon(true);
					processFrame.setName("processFrame");
					processFrame.start();

					// update the button content
					this.button.setText("Stop Camera");
				} else {
					// log the error
					throw new Exception("Impossible to open the camera connection");
				}
			} else {
				// the camera is not active at this point
				cameraActive = false;
				// update again the button content
				this.button.setText("Start Camera");
			}
		} catch (Exception e) {
			e.printStackTrace();
			cameraActive = false;
			this.button.setText("Start Camera");
		}
	}

	/**
	 * Always Update UI from main thread
	 */
	private void setFrametoImageView(Image frame) {
		Platform.runLater(() -> {
			currentFrame.setImage(frame);
			currentFrame.setFitWidth(ImageViewPane.getWidth());
			currentFrame.setFitHeight((ImageViewPane.getHeight()));
			// set Image height/width by window size
		});

	}
	
	public Mat detect(Mat inputframe){  
        Mat mRgba=new Mat();  
        Mat mGrey=new Mat();  
        MatOfRect faces = new MatOfRect();  
        
        inputframe.copyTo(mRgba);  
        inputframe.copyTo(mGrey);  
        
        Imgproc.cvtColor( mRgba, mGrey, Imgproc.COLOR_BGR2GRAY);  
        Imgproc.equalizeHist( mGrey, mGrey );  
        
        face_cascade.detectMultiScale(mGrey, faces);  
        detectedFaces = faces.toArray().length;
        
        System.out.println(String.format("Detected %s faces", detectedFaces));  
        
        for(Rect rect:faces.toArray()){  
             Point center= new Point(rect.x + rect.width*0.5, rect.y + rect.height*0.5 );  
             Imgproc.ellipse( mRgba, center, new Size( rect.width*0.5, rect.height*0.5), 0, 0, 360, new Scalar( 255, 0, 255 ), 4, 8, 0 );
             //Core.rectangle(mRgba, new Point(rect.width*0.5, rect.height*0.5), center, new Scalar( 0, 255, 255 ), 4, 8, 0);
             croppedImage = Mat2BufferedImage(mGrey);
        }  
        return mRgba;  
   }
   
   public BufferedImage Mat2BufferedImage(Mat m){
   	// source: http://answers.opencv.org/question/10344/opencv-java-load-image-to-gui/
   	// Fastest code
   	// The output can be assigned either to a BufferedImage or to an Image

   	    int type = BufferedImage.TYPE_BYTE_GRAY;
   	    if ( m.channels() > 1 ) {
   	        type = BufferedImage.TYPE_3BYTE_BGR;
   	    }
   	    int bufferSize = m.channels()*m.cols()*m.rows();
   	    byte [] b = new byte[bufferSize];
   	    m.get(0,0,b); // get all the pixels
   	    BufferedImage image = new BufferedImage(m.cols(),m.rows(), type);
   	    final byte[] targetPixels = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
   	    System.arraycopy(b, 0, targetPixels, 0, b.length);  
   	    return image;

   	}

	/**
	 * Always Update UI from main thread
	 */
	private void updateUIObjects() {
		Platform.runLater(() -> {
			// Update UI Objects like: Textfield.setText() , Button.set..() ,
			// Window.Resize...()
			//Set FPS
			fps.setText(""+capture.get(5));
		});
	}

	/**
	 * Get a frame from the opened video stream (if any)
	 *
	 * @return the {@link Mat} to show
	 */
	private Mat grabFrame() {
		// init everything
		Mat frame = new Mat();

		// check if the capture is open
		if (this.capture.isOpened()) {
			try {
				// read the current frame
				this.capture.read(frame);

				// if the frame is not empty, process it
				if (!frame.empty()) {
					
				}

			} catch (Exception e) {
				// log the error
				System.err.println("Exception during the image elaboration: " + e);
			}
		}

		return frame;
	}

	/**
	 * Process a Frame
	 *
	 * @return the {@link Image} to show
	 */
	private Mat processMat(Mat matToShow) {
		// convert the image to gray scale
		//Imgproc.cvtColor(matToShow, matToShow, Imgproc.COLOR_BGR2GRAY);
		return matToShow;
	}

	/**
	 * Convert a Mat object (OpenCV) in the corresponding Image for JavaFX
	 *
	 * @param frame
	 *            the {@link Mat} representing the current frame
	 * @return the {@link Image} to show
	 */
	private Image mat2Image(Mat frame) {
		try {
			return SwingFXUtils.toFXImage(matToBufferedImage(frame), null);
		} catch (Exception e) {
			System.out.println("Cant convert mat" + e);
			return null;
		}
	}

	public BufferedImage matToBufferedImage(Mat matBGR) {
		int width = matBGR.width(), height = matBGR.height(), channels = matBGR.channels();
		byte[] sourcePixels = new byte[width * height * channels];
		matBGR.get(0, 0, sourcePixels);
		BufferedImage image;
		if (matBGR.channels() > 1) {
			image = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
		} else {
			image = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
		}
		final byte[] targetPixels = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
		System.arraycopy(sourcePixels, 0, targetPixels, 0, sourcePixels.length);
		return image;
	}

	public void setClosed() {
		//Close thread on window close
		cameraActive = false;
	}

}
