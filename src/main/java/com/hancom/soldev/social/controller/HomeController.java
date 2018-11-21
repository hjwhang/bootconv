package com.hancom.soldev.social.controller;

import java.io.File;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hancom.soldev.social.service.ConvService;

//@Controller
@RestController
public class HomeController {
	private static final Logger logger = LoggerFactory.getLogger(HomeController.class);
	@Autowired
	ConvService conv;
	
	// http://localhost:8080/test?file=TBK_2017_00120_5_1592.txt
	@GetMapping("/test")
	public String test (String file) {
		String path = "/Users/hjwhang/Downloads/tbook_cnts_html";
		String hwpPath = "/Users/hjwhang/Downloads/tbook_cnts_html/test.hwp";
		File f = new File(path, file);
		try {
			String html = conv.loadHtml(f.getAbsolutePath());
			List<String> urls = conv.parsingImg(html);
			String html2 = conv.convImgTag(html, urls);
			String htmlPath = conv.saveHtml(html2);
			conv.downloadImages(urls, htmlPath);
			String zipPath = conv.zipHtml(htmlPath);
			conv.convToHwp(zipPath, hwpPath);
			conv.deleteDirectory(htmlPath);
			conv.deleteFile(zipPath);
		}
		catch(Exception e) {
			logger.error("err: {}", e);
		}
		return "ok";
	}
}
