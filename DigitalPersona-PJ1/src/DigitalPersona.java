import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.EnumMap;
import java.util.concurrent.LinkedBlockingQueue;

import com.digitalpersona.onetouch.*;
import com.digitalpersona.onetouch.capture.DPFPCapture;
import com.digitalpersona.onetouch.capture.DPFPCapturePriority;
import com.digitalpersona.onetouch.capture.event.DPFPDataEvent;
import com.digitalpersona.onetouch.capture.event.DPFPDataListener;
import com.digitalpersona.onetouch.capture.event.DPFPReaderStatusAdapter;
import com.digitalpersona.onetouch.capture.event.DPFPReaderStatusEvent;
import com.digitalpersona.onetouch.processing.DPFPEnrollment;
import com.digitalpersona.onetouch.processing.DPFPFeatureExtraction;
import com.digitalpersona.onetouch.processing.DPFPImageQualityException;
import com.digitalpersona.onetouch.readers.DPFPReaderDescription;
import com.digitalpersona.onetouch.readers.DPFPReadersCollection;
import com.digitalpersona.onetouch.ui.swing.*;
import com.digitalpersona.onetouch.verification.DPFPVerification;
import com.digitalpersona.onetouch.verification.DPFPVerificationResult;

public class DigitalPersona {
	//static EnumMap<DPFPFingerIndex, DPFPTemplate> templates;
	static EnumMap<DPFPFingerIndex, DPFPTemplate> templates = new EnumMap<DPFPFingerIndex, DPFPTemplate>(DPFPFingerIndex.class);

	public static void main(String[] args) {
		listReaders();
		DigitalPersona dp = new DigitalPersona();
		DPFPTemplate temp = dp.getTemplate(null, 1);
		byte[] b = temp.serialize();
		dp.insert(1, b);
		
		b = dp.get();
		
		
		DPFPTemplate temp2 = DPFPGlobal.getTemplateFactory().createTemplate();
		temp2.deserialize(b);
		
		if (dp.verify(null, temp))
			System.out.println("Finger Verified!");
		else 
			System.out.println("Finger not verify!");
		
		
		//boolean verify = dp.verify(null, template);
		
		//System.out.println(verify);
		//DPFPEnrollmentEvent e = null;
		
		//templates.put(e.getFingerIndex(), e.getTemplate());

	}
	
	public Connection cn() {
		Connection conn = null;
		try { Class.forName("com.mysql.jdbc.Driver");
		conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/fingerprint", "root", "root");
		} catch(Exception e) { System.out.println(e); }
		
		return conn;
	}
	
	public void insert(int id, byte[] digital){
		PreparedStatement st;
		try { 
			st = cn().prepareStatement("INSERT INTO users(username, f1) VALUES(?, ?)");
			st.setInt(1, id);
			st.setBytes(2, digital);
			st.executeUpdate();
		} catch (SQLException e) {  System.out.println(e.getMessage()); } 
	}
	
	public byte[] get(){ 
		ResultSet rs;
		PreparedStatement st;
		byte[] digital = null;
		try { 
			st = cn().prepareStatement("SELECT * FROM users");
			rs = st.executeQuery();
			if(rs.next())
				digital = rs.getBytes("f1");
			else 
				System.out.println("Record not available");
			 
		} catch (Exception e) {
			System.out.println(e.getMessage());
		} 
		 
		return digital;
	} 
	
	public static void listReaders() { 
        DPFPReadersCollection readers = DPFPGlobal.getReadersFactory().getReaders();
        if (readers == null || readers.size() == 0) {
            System.out.printf("There are no readers available.\n");
            return; 
        } 
        System.out.printf("Available readers:\n");
        for (DPFPReaderDescription readerDescription : readers)
            System.out.println(readerDescription.getSerialNumber());
    } 

	public static final EnumMap<DPFPFingerIndex, String> fingerNames;
    static { 
    	fingerNames = new EnumMap<DPFPFingerIndex, String>(DPFPFingerIndex.class);
    	fingerNames.put(DPFPFingerIndex.LEFT_PINKY,	  "left pinky");
    	fingerNames.put(DPFPFingerIndex.LEFT_RING,    "left ring");
    	fingerNames.put(DPFPFingerIndex.LEFT_MIDDLE,  "left middle");
    	fingerNames.put(DPFPFingerIndex.LEFT_INDEX,   "left index");
    	fingerNames.put(DPFPFingerIndex.LEFT_THUMB,   "left thumb");
    	fingerNames.put(DPFPFingerIndex.RIGHT_PINKY,  "right pinky");
    	fingerNames.put(DPFPFingerIndex.RIGHT_RING,   "right ring");
    	fingerNames.put(DPFPFingerIndex.RIGHT_MIDDLE, "right middle");
    	fingerNames.put(DPFPFingerIndex.RIGHT_INDEX,  "right index");
    	fingerNames.put(DPFPFingerIndex.RIGHT_THUMB,  "right thumb");
    } 
    
	public DPFPTemplate getTemplate(String activeReader, int nFinger) {
        System.out.printf("Performing fingerprint enrollment...\n");
         
        DPFPTemplate template = null;
         
        try { 
            DPFPFingerIndex finger = DPFPFingerIndex.values()[nFinger];
            DPFPFeatureExtraction featureExtractor = DPFPGlobal.getFeatureExtractionFactory().createFeatureExtraction();
            DPFPEnrollment enrollment = DPFPGlobal.getEnrollmentFactory().createEnrollment();
             
            while (enrollment.getFeaturesNeeded() > 0)
            { 
                DPFPSample sample = getSample(activeReader, 
                	String.format("Scan your %s finger (%d remaining)\n", fingerName(finger), enrollment.getFeaturesNeeded()));
                if (sample == null)
                    continue; 
 
 
                DPFPFeatureSet featureSet;
                try { 
                    featureSet = featureExtractor.createFeatureSet(sample, DPFPDataPurpose.DATA_PURPOSE_ENROLLMENT);
                } catch (DPFPImageQualityException e) {
                    System.out.printf("Bad image quality: \"%s\". Try again. \n", e.getCaptureFeedback().toString());
                    continue; 
                } 
 
 
                enrollment.addFeatures(featureSet);
            } 
            template = enrollment.getTemplate();
            System.out.printf("The %s was enrolled.\n", fingerprintName(finger));
        } catch (DPFPImageQualityException e) {
            System.out.printf("Failed to enroll the finger.\n");
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } 
         
        return template;
    }
	
	public boolean verify(String activeReader, DPFPTemplate template) {
		 
		 
        try { 
            DPFPSample sample = getSample(activeReader, "Scan your finger\n");
            if (sample == null)
                throw new Exception();
 
 
            DPFPFeatureExtraction featureExtractor = DPFPGlobal.getFeatureExtractionFactory().createFeatureExtraction();
            DPFPFeatureSet featureSet = featureExtractor.createFeatureSet(sample, DPFPDataPurpose.DATA_PURPOSE_VERIFICATION);
			 
            DPFPVerification matcher = DPFPGlobal.getVerificationFactory().createVerification();
            matcher.setFARRequested(DPFPVerification.MEDIUM_SECURITY_FAR);
             
            for (DPFPFingerIndex finger : DPFPFingerIndex.values()) {
                //DPFPTemplate template = user.getTemplate(finger); 
                if (template != null) {
                    DPFPVerificationResult result = matcher.verify(featureSet, template);
                    if (result.isVerified()) {
                        System.out.printf("Matching finger: %s, FAR achieved: %g.\n",
                        		fingerName(finger), (double)result.getFalseAcceptRate()/DPFPVerification.PROBABILITY_ONE);
                        return result.isVerified();
                    } 
                } 
            } 
        } catch (Exception e) {
            System.out.printf("Failed to perform verification.");
        } 
         
        return false; 
    } 
 
 
    public DPFPSample getSample(String activeReader, String prompt)
	throws InterruptedException
	{ 
	    final LinkedBlockingQueue<DPFPSample> samples = new LinkedBlockingQueue<DPFPSample>();
	    DPFPCapture capture = DPFPGlobal.getCaptureFactory().createCapture();
	    capture.setReaderSerialNumber(activeReader);
	    capture.setPriority(DPFPCapturePriority.CAPTURE_PRIORITY_LOW);
	    capture.addDataListener(new DPFPDataListener()
	    { 
	        public void dataAcquired(DPFPDataEvent e) {
	            if (e != null && e.getSample() != null) {
	                try { 
	                    samples.put(e.getSample());
	                } catch (InterruptedException e1) {
	                    e1.printStackTrace();
	                } 
	            } 
	        } 
	    }); 
	    capture.addReaderStatusListener(new DPFPReaderStatusAdapter()
	    { 
	    	int lastStatus = DPFPReaderStatusEvent.READER_CONNECTED;
			public void readerConnected(DPFPReaderStatusEvent e) {
				if (lastStatus != e.getReaderStatus())
					System.out.println("Reader is connected");
				lastStatus = e.getReaderStatus();
			} 
			public void readerDisconnected(DPFPReaderStatusEvent e) {
				if (lastStatus != e.getReaderStatus())
					System.out.println("Reader is disconnected");
				lastStatus = e.getReaderStatus();
			} 
	    	 
	    }); 
	    try { 
	        capture.startCapture();
	        System.out.print(prompt);
	        return samples.take();
	    } catch (RuntimeException e) {
	        System.out.printf("Failed to start capture. Check that reader is not used by another application.\n");
	        throw e;
	    } finally { 
	        capture.stopCapture();
	    } 
	} 
 
 
    public String fingerName(DPFPFingerIndex finger) {
    	return fingerNames.get(finger); 
    } 
    public String fingerprintName(DPFPFingerIndex finger) {
    	return fingerNames.get(finger) + " fingerprint"; 
    } 
}
