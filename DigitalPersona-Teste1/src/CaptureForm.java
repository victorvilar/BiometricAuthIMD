
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.HeadlessException;
import java.awt.Image;
import java.awt.Transparency;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.PixelGrabber;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import com.digitalpersona.onetouch.DPFPCaptureFeedback;
import com.digitalpersona.onetouch.DPFPDataPurpose;
import com.digitalpersona.onetouch.DPFPFeatureSet;
import com.digitalpersona.onetouch.DPFPGlobal;
import com.digitalpersona.onetouch.DPFPSample;
import com.digitalpersona.onetouch.capture.DPFPCapture;
import com.digitalpersona.onetouch.capture.event.DPFPDataAdapter;
import com.digitalpersona.onetouch.capture.event.DPFPDataEvent;
import com.digitalpersona.onetouch.capture.event.DPFPImageQualityAdapter;
import com.digitalpersona.onetouch.capture.event.DPFPImageQualityEvent;
import com.digitalpersona.onetouch.capture.event.DPFPReaderStatusAdapter;
import com.digitalpersona.onetouch.capture.event.DPFPReaderStatusEvent;
import com.digitalpersona.onetouch.capture.event.DPFPSensorAdapter;
import com.digitalpersona.onetouch.capture.event.DPFPSensorEvent;
import com.digitalpersona.onetouch.processing.DPFPFeatureExtraction;
import com.digitalpersona.onetouch.processing.DPFPImageQualityException;

public class CaptureForm extends JDialog {
	private DPFPCapture capturer = DPFPGlobal.getCaptureFactory().createCapture();
	private JLabel picture = new JLabel();
	private JTextField prompt = new JTextField();
	private JTextArea log = new JTextArea();
	private JTextField status = new JTextField("[status line]");
	private int imageCount = 0;

	public CaptureForm(Frame owner) {
		super(owner, true);
		setTitle("Fingerprint Enrollment");

		setLayout(new BorderLayout());
		rootPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

		picture.setPreferredSize(new Dimension(240, 280));
		picture.setBorder(BorderFactory.createLoweredBevelBorder());
		prompt.setFont(UIManager.getFont("Panel.font"));
		prompt.setEditable(false);
		prompt.setColumns(40);
		prompt.setMaximumSize(prompt.getPreferredSize());
		prompt.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0), "Prompt:"),
				BorderFactory.createLoweredBevelBorder()));
		log.setColumns(40);
		log.setEditable(false);
		log.setFont(UIManager.getFont("Panel.font"));
		JScrollPane logpane = new JScrollPane(log);
		logpane.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0), "Status:"),
				BorderFactory.createLoweredBevelBorder()));

		status.setEditable(false);
		status.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		status.setFont(UIManager.getFont("Panel.font"));

		JButton quit = new JButton("Close");
		quit.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				setVisible(false);
			}
		});

		JPanel right = new JPanel(new BorderLayout());
		right.setBackground(Color.getColor("control"));
		right.add(prompt, BorderLayout.PAGE_START);
		right.add(logpane, BorderLayout.CENTER);

		JPanel center = new JPanel(new BorderLayout());
		center.setBackground(Color.getColor("control"));
		center.add(right, BorderLayout.CENTER);
		center.add(picture, BorderLayout.LINE_START);
		center.add(status, BorderLayout.PAGE_END);

		JPanel bottom = new JPanel(new FlowLayout(FlowLayout.TRAILING));
		bottom.setBackground(Color.getColor("control"));
		bottom.add(quit);

		setLayout(new BorderLayout());
		add(center, BorderLayout.CENTER);
		add(bottom, BorderLayout.PAGE_END);

		this.addComponentListener(new ComponentAdapter() {
			@Override
			public void componentShown(ComponentEvent e) {
				init();
				start();
			}

			@Override
			public void componentHidden(ComponentEvent e) {
				stop();
			}

		});

		pack();
		setLocationRelativeTo(null);
	}

	protected void init() {
		capturer.addDataListener(new DPFPDataAdapter() {
			@Override
			public void dataAcquired(final DPFPDataEvent e) {
				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						makeReport("The fingerprint sample was captured.");
						setPrompt("Scan the same fingerprint again.");
						process(e.getSample());
					}
				});
			}
		});
		capturer.addReaderStatusListener(new DPFPReaderStatusAdapter() {
			@Override
			public void readerConnected(final DPFPReaderStatusEvent e) {
				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						makeReport("The fingerprint reader was connected.");
					}
				});
			}

			@Override
			public void readerDisconnected(final DPFPReaderStatusEvent e) {
				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						makeReport("The fingerprint reader was disconnected.");
					}
				});
			}
		});
		capturer.addSensorListener(new DPFPSensorAdapter() {
			@Override
			public void fingerTouched(final DPFPSensorEvent e) {
				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						makeReport("The fingerprint reader was touched.");
					}
				});
			}

			@Override
			public void fingerGone(final DPFPSensorEvent e) {
				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						makeReport("The finger was removed from the fingerprint reader.");
					}
				});
			}
		});
		capturer.addImageQualityListener(new DPFPImageQualityAdapter() {
			@Override
			public void onImageQuality(final DPFPImageQualityEvent e) {
				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						if (e.getFeedback().equals(DPFPCaptureFeedback.CAPTURE_FEEDBACK_GOOD))
							makeReport("The quality of the fingerprint sample is good.");
						else
							makeReport("The quality of the fingerprint sample is poor.");
					}
				});
			}
		});
	}

	protected void process(DPFPSample sample) {
		// Draw fingerprint sample image.
		Image image = convertSampleToBitmap(sample);

		File outputFile = new File("D:/Dropbox/UFRN/2016.1/DAD/imageTeste" + imageCount + ".jpg");
		imageCount++;
		BufferedImage input = toBufferedImage(image);
		try {
			ImageIO.write(input, "JPG", outputFile);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		drawPicture(convertSampleToBitmap(sample));
	}

	protected void start() {
		capturer.startCapture();
		setPrompt("Using the fingerprint reader, scan your fingerprint.");
	}

	protected void stop() {
		capturer.stopCapture();
	}

	public void setStatus(String string) {
		status.setText(string);
	}

	public void setPrompt(String string) {
		prompt.setText(string);
	}

	public void makeReport(String string) {
		log.append(string + "\n");
	}

	public void drawPicture(Image image) {
		picture.setIcon(
				new ImageIcon(image.getScaledInstance(picture.getWidth(), picture.getHeight(), Image.SCALE_DEFAULT)));
	}

	protected Image convertSampleToBitmap(DPFPSample sample) {
		return DPFPGlobal.getSampleConversionFactory().createImage(sample);
	}

	protected DPFPFeatureSet extractFeatures(DPFPSample sample, DPFPDataPurpose purpose) {
		DPFPFeatureExtraction extractor = DPFPGlobal.getFeatureExtractionFactory().createFeatureExtraction();
		try {
			return extractor.createFeatureSet(sample, purpose);
		} catch (DPFPImageQualityException e) {
			return null;
		}
	}

	public static BufferedImage toBufferedImage(Image image) {
        if (image instanceof BufferedImage) {
            return (BufferedImage)image;
        }
        // This code ensures that all the pixels in the image are loaded
        image = new ImageIcon(image).getImage();
        // Determine if the image has transparent pixels; for this method's
        // implementation, see e661 Determining If an Image Has Transparent Pixels
        boolean hasAlpha = hasAlpha(image);
        // Create a buffered image with a format that's compatible with the screen
        BufferedImage bimage = null;
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        try {
            // Determine the type of transparency of the new buffered image
            int transparency = Transparency.OPAQUE;
            if (hasAlpha) {
                transparency = Transparency.BITMASK;
            }
            // Create the buffered image
            GraphicsDevice gs = ge.getDefaultScreenDevice();
            GraphicsConfiguration gc = gs.getDefaultConfiguration();
            bimage = gc.createCompatibleImage(
                image.getWidth(null), image.getHeight(null), transparency);
        } catch (HeadlessException e) {
            // The system does not have a screen
        }
        if (bimage == null) {
            // Create a buffered image using the default color model
            int type = BufferedImage.TYPE_INT_RGB;
            if (hasAlpha) {
                type = BufferedImage.TYPE_INT_ARGB;
            }
            bimage = new BufferedImage(image.getWidth(null), image.getHeight(null), type);
        }
        // Copy image to buffered image
        Graphics g = bimage.createGraphics();
        // Paint the image onto the buffered image
        g.drawImage(image, 0, 0, null);
        g.dispose();
        return bimage;
    }
	
	public static boolean hasAlpha(Image image) {
        // If buffered image, the color model is readily available
        if (image instanceof BufferedImage) {
            BufferedImage bimage = (BufferedImage)image;
            return bimage.getColorModel().hasAlpha();
        }
        // Use a pixel grabber to retrieve the image's color model;
        // grabbing a single pixel is usually sufficient
         PixelGrabber pg = new PixelGrabber(image, 0, 0, 1, 1, false);
        try {
            pg.grabPixels();
        } catch (InterruptedException e) {
        }
        // Get the image's color model
        ColorModel cm = pg.getColorModel();
        return cm.hasAlpha();
    }
	
}
