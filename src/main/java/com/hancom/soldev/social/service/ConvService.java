package com.hancom.soldev.social.service;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.FileUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.InputStreamBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

//import net.lingala.zip4j.core.ZipFile;
//import net.lingala.zip4j.model.ZipParameters;
//import net.lingala.zip4j.util.Zip4jConstants;

@Service
public class ConvService {
	private static final Logger logger = LoggerFactory.getLogger(ConvService.class);
	String convUrl; // html2hwp url
	String imgPath = "/img";
	/**
	 * 		<!-- zip -->
	 		<dependency>
		    <groupId>net.lingala.zip4j</groupId>
		    <artifactId>zip4j</artifactId>
		    <version>1.3.2</version>
		</dependency>
		
		<!-- https://mvnrepository.com/artifact/org.apache.httpcomponents/httpmime -->
<dependency>
    <groupId>org.apache.httpcomponents</groupId>
    <artifactId>httpmime</artifactId>
    <version>4.1.2</version>
</dependency>
	 */
	
	/**
	 * 1. 에디터로 편집된 object 가져오기
    ** 받아 온 것으로 가정
    
2. object -> html (html generator)
    ** 변환한 것으로 가정
3. html -> 이미지와 css 파일 목록 추출하여 목표 폴더에 파일 복사
    목표 폴더는 변수로 처리 (후에 이 경로만 바꾸면 다른 환경에서도 사용할 수 있도록)
    
4. 변환용 파일 압축
5. 변환 요청 (변환 서버)
6. 결과물 파일 복사
    ** 비개발
7. 임시 파일 삭제
	 */
	
	/***
	 * html file loading
	 * @param path
	 * @return html contents
	 */
	public String loadHtml(String path) {
		StringBuilder html = new StringBuilder();
		try {
			BufferedReader in = new BufferedReader(
					   new InputStreamReader(
			                      new FileInputStream(path), "UTF8"));
			
			String str;
			while ((str = in.readLine()) != null) {
			    html.append(str);
			}
			        
	         in.close();
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return html.toString();
	}
	
	/**
	 * <img> 태크 추츨 
	 * @param html -html contents
	 * @return img url list
	 */
	public List<String> parsingImg(String html) {
		List<String> urls = new ArrayList<String>();
		
		String pattern = "<img src=\"(.+?)\"";
		Pattern p = Pattern.compile(pattern);
		Matcher m = p.matcher(html);
		while (m.find()) {
//			int count = m.groupCount();
			String t = m.group(1);
//			t = html.substring(m.start(), m.end());
			if (urls.contains(t) == false)
				urls.add(t);
		}
		return urls;
	}
	
	String transPath (String path) {
		String fileName = getFileNameFromUrl(path);
		File destFile = new File("." + imgPath, fileName);
		return destFile.getPath();
	}
	
	/**
	 * <img> url 변환 
	 * @param html - html contents
	 * @param files - 변경할 url path
	 * @return 변경된 html contents
	 */
	public String convImgTag(String html, List<String> urls) {
		
		for (String imgPath : urls) {
			String newPath = transPath(imgPath);
			html = html.replace(imgPath, newPath);
		}
		return html;
	}
	
	// html 파일 저장 
	// return html folder path
	
	/**
	 * html 파일 저장 (UTF-8로 저장)
	 * @param html 저장할 html
	 * @return 저장된 path (exclude filename)
	 */
	public String saveHtml(String html) {
		File dest = null;  // dest Folder
		File indexFile = null; // index.html
		OutputStream os = null;
		try {
			dest = createTempDirectory();
			indexFile = new File(dest, "index.html");
			
			BufferedWriter out = new BufferedWriter(new OutputStreamWriter(
				    new FileOutputStream(indexFile), "UTF-8"));
			try {
			    out.write(html);
			} finally {
			    out.close();
			}
			
		}
		catch (Exception e) {
			
		}
		finally {
			closeStream(os);
		}
		
		return dest.getAbsolutePath();
	}
	
	String getFileNameFromUrl(String imageUrl) {
		String fileName = "";
		try {
			String urlImg = new URL(imageUrl).getFile();
			fileName = urlImg.substring( urlImg.lastIndexOf('/')+1, urlImg.length() ); 
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return fileName;
	}
	
	void saveImage(String imageUrl, String destPath)  {
		URL url;
		InputStream is = null;
		OutputStream os = null;
		try {
			url = new URL(imageUrl);
			String fileName = getFileNameFromUrl(imageUrl);
			File destFile = new File(destPath, fileName);
			//destFile.createNewFile();
			is = url.openStream();
			os = new FileOutputStream(destFile);

			byte[] b = new byte[2048];
			int length;

			while ((length = is.read(b)) != -1) {
				os.write(b, 0, length);
			}			
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		catch (Exception e) {
			logger.error("err: {}", e);
		}
		finally {
			closeStream(is);
			closeStream(os);			
		}

	}

	/**
	 * 이미지 파일 다운로드 
	 * @param files - image file url list
	 * @param path - download folder path
	 */
	public void downloadImages(List<String> urls, String path) {
		String downPath = path + imgPath;
		try {
			FileUtils.forceMkdir(new File(downPath));
			for (String file : urls) {
				saveImage(file, downPath);
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/**
	 * zip html folder
	 * @param path - folder path
	 * @return zip file path
	 */
	public String zipHtml(String path) {
		File zipFile = null;;
		try {
			zipFile = File.createTempFile("temp", ".zip");
			archiveDir(zipFile.getAbsolutePath(), path);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return zipFile.getAbsolutePath();
	}
	
	HttpResponse uploadFile(String srcPath) {
		File f = new File(srcPath);
		
		FileInputStream fis = null;
		HttpResponse response = null;
		try {
			fis = new FileInputStream(f);
			DefaultHttpClient httpclient = new DefaultHttpClient(new BasicHttpParams());
			// server back-end URL
			HttpPost httppost = new HttpPost(convUrl);
			MultipartEntity entity = new MultipartEntity();
			// set the file input stream and file name as arguments
			entity.addPart("file", new InputStreamBody(fis, f.getName()));
			httppost.setEntity(entity);
			// execute the request
			response = httpclient.execute(httppost);
			
		} catch (ClientProtocolException e) {
			System.err.println("Unable to make connection");
			e.printStackTrace();
		} catch (IOException e) {
			System.err.println("Unable to read file");
			e.printStackTrace();
		} finally {
			closeStream(fis);
		}
		return response;
	}
	
	void saveHwp(HttpEntity entity, String destPath) {
		FileOutputStream fos = null;
		InputStream is = null;
		try {
			is = entity.getContent();
			fos = new FileOutputStream(destPath);
			int inByte;
			while((inByte = is.read()) != -1)
			     fos.write(inByte);
		} catch (IllegalStateException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		finally {
			closeStream(is);
			closeStream(fos);
		}
	}
	
	/**
	 * 변환서버 호출 
	 * @param srcPath - html zip file path
	 * @param destPath - save to hwp file path
	 */
	public void convToHwp(String zipPath, String hwpPath) {
		HttpResponse response = uploadFile(zipPath);
		
		int statusCode = response.getStatusLine().getStatusCode();
		if (statusCode == 200) {
			HttpEntity entity = response.getEntity();
			saveHwp(entity, hwpPath);
		}
		else {
			// not ok
		}
	}
	
	/**
	 * 임시 폴더 삭제 
	 * @param path - 삭제할 폴더 경로 
	 */
	public void deleteDirectory(String path) 
	{		
		try {
			FileUtils.deleteDirectory(new File(path));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			logger.error("error: {}", e);
		}
	}
	
	/**
	 * 임시 zip파일 삭제 
	 * @param file
	 * @return
	 */
	public boolean deleteFile(String file) {
		File f = new File(file);
		return f.delete();
	}
	
	void closeStream(InputStream is) {
        try {
	        	if (is != null)
	        		is.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	void closeStream(OutputStream os) {
        try {
        	if (os != null)
        		os.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	void zipDir(String dir2zip, ZipOutputStream zos, String zipPath) 
	{ 
		try 
		{ 
	        //create a new File object based on the directory we 
	        //have to zip File    
	        File zipDir = new File(dir2zip); 
	        //get a listing of the directory content 
	        String[] fileList = zipDir.list(); 
	        byte[] readBuffer = new byte[2156]; 
	        int bytesIn = 0; 
	        //loop through dirList, and zip the files 
	        for (String file : fileList) {
	        //for(int i=0; i<dirList.length; i++)  { 
	            File f = new File(zipDir, file); 
		        if(f.isDirectory()) 
		        { 
		                //if the File object is a directory, call this 
		                //function again to add its content recursively 
		            String filePath = f.getPath(); 
		            zipPath += f.getName() + "/";
		            zipDir(filePath, zos, zipPath); 
		                //loop again 
		            continue; 
		        } 
		        else {
		            //if we reached here, the File object f was not 
		            //a directory 
		            //create a FileInputStream on top of f 
		            FileInputStream fis = new FileInputStream(f); 
		            //create a new zip entry 
		            String entry = zipPath + f.getName();
			        ZipEntry anEntry = new ZipEntry(entry); 
			            //place the zip entry in the ZipOutputStream object 
			        zos.putNextEntry(anEntry); 
		            //now write the content of the file to the ZipOutputStream 
		            while((bytesIn = fis.read(readBuffer)) != -1) 
		            { 
		                zos.write(readBuffer, 0, bytesIn); 
		            } 
		           //close the Stream 
		           closeStream(fis);
		        }
	        }
		}

		catch(Exception e) 
		{ 
		    //handle exception 
		} 
	}
	
	void archiveDir(String zipFileName, String path) {
		try 
		{ 
		    //create a ZipOutputStream to zip the data to 
		    ZipOutputStream zos = new 
		           ZipOutputStream(new FileOutputStream(zipFileName)); 
		    //assuming that there is a directory named inFolder (If there 
		    //isn't create one) in the same directory as the one the code 
		    //runs from, 
		    //call the zipDir method 
		    zipDir(path, zos, ""); 
		    //close the stream 
		    zos.close(); 
		} 
		catch(Exception e) 
		{ 
		    //handle exception 
		} 
	}
//	
//	static void archiveDir(String zipFileName, String path)  throws Exception {
////		  Calendar calendar = Calendar.getInstance();
////		  Date time = calendar.getTime();
////		  long milliseconds = time.getTime();
//
//		  // Initiate ZipFile object with the path/name of the zip file.
//		  ZipFile zipFile = new ZipFile(zipFileName);
//
//		  // Folder to add
//		  String folderToAdd = path;
//
//		  // Initiate Zip Parameters which define various properties such
//		  // as compression method, etc.
//		  ZipParameters parameters = new ZipParameters();
//
//		  // set compression method to store compression
//		  parameters.setCompressionMethod(Zip4jConstants.COMP_DEFLATE);
//
//		  // Set the compression level
//		  parameters.setCompressionLevel(Zip4jConstants.DEFLATE_LEVEL_NORMAL);
//
//		  // Add folder to the zip file
//		  zipFile.addFolder(folderToAdd, parameters);
//
////		 } catch (Exception e) {
////		  e.printStackTrace();
////		 }
//	}
	
	static File createTempDirectory()
		    throws IOException {
	    final File temp;
	    temp = File.createTempFile("temp", Long.toString(System.nanoTime()));

	    if(!(temp.delete()))
	    {
	        throw new IOException("Could not delete temp file: " + temp.getAbsolutePath());
	    }

	    if(!(temp.mkdir()))
	    {
	        throw new IOException("Could not create temp directory: " + temp.getAbsolutePath());
	    }

	    return (temp);
	}
}
