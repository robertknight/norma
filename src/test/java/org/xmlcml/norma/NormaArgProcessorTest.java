package org.xmlcml.norma;

import java.io.File;
import java.io.IOException;
import java.util.List;

import junit.framework.Assert;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.Ignore;
import org.junit.Test;
import org.xmlcml.cmine.args.ArgumentOption;
import org.xmlcml.cmine.args.DefaultArgProcessor;
import org.xmlcml.cmine.files.CMDir;
import org.xmlcml.cmine.files.CMDirList;

public class NormaArgProcessorTest {

	
	private static final Logger LOG = Logger
			.getLogger(NormaArgProcessorTest.class);
	static {
		LOG.setLevel(Level.DEBUG);
	}
	
	@Test
	@Ignore // as this just prints help
	public void testNoArgs() {
		String[] args = {
		};
		new NormaArgProcessor(args);
	}
	
	@Test // outputs help
	@Ignore // to cut down output
	public void testNoArgsMain() {
		String[] args = {
		};
		Norma.main(args);
		Assert.assertTrue("finished", true);
		LOG.trace("finished");
	}
	

	@Test
	public void testArgs() {
		FileUtils.deleteQuietly(new File("foo"));
		FileUtils.deleteQuietly(new File("bar"));
		String[] args = {
			"-i", "foo", "bar", 
			"-o", "plugh",
		};
		DefaultArgProcessor argProcessor = new NormaArgProcessor(args);
		Assert.assertEquals("output", "plugh", argProcessor.getOutput());
		Assert.assertEquals("input", 2, argProcessor.getInputList().size());
		Assert.assertEquals("input", "foo", argProcessor.getInputList().get(0));
		Assert.assertEquals("input", "bar", argProcessor.getInputList().get(1));
	}
	
	@Test
	public void testPDF() {
		String[] args = {
			"-i", new File(Fixtures.TEST_BMC_DIR, "s12862-014-0277-x.pdf").toString(),
			"-o", "plugh",
		};
		DefaultArgProcessor argProcessor = new NormaArgProcessor(args);
		Assert.assertEquals("output", "plugh", argProcessor.getOutput());
		Assert.assertEquals("input", 1, argProcessor.getInputList().size());
	}
	
	@Test
	public void testAutoDetect() {
		String[] args = {
			"-i", new File(Fixtures.TEST_BMC_DIR, "s12862-014-0277-x.pdf").toString(),
			"-o", "plugh",
		};
		DefaultArgProcessor argProcessor = new NormaArgProcessor(args);
		Assert.assertEquals("output", "plugh", argProcessor.getOutput());
		Assert.assertEquals("input", 1, argProcessor.getInputList().size());
	}
	
	@Test
	public void testMethod() {
		String inputFilename =  new File(Fixtures.TEST_BMC_DIR, "s12862-014-0277-x.pdf").toString();
		String[] args = {
			"-i", inputFilename,
			"-o", "plugh",
			"-p", "bmc",
		};
		NormaArgProcessor argProcessor = new NormaArgProcessor(args);
		Assert.assertEquals("input", 1, argProcessor.getInputList().size());
		Assert.assertEquals("input",  inputFilename, argProcessor.getInputList().get(0));
		Assert.assertEquals("output", "plugh", argProcessor.getOutput());
		Assert.assertEquals("pubstyle", "bmc", argProcessor.getPubstyle().toString());
	}
	
	@Test
	@Ignore // because it is voluminous
	public void testHelp() {
		DefaultArgProcessor argProcessor = new NormaArgProcessor(new String[]{"-h"});
	}
	
	@Test
	/** normalizes an XML file and writes out shtml.
	 * 
	 *  // SHOWCASE
	 * Not fully tagged. this is to test directory mechanism.
	 * 
	 * @throws IOException
	 */
	public void testCreateScholarlyHtml() throws IOException {
		File container0115884 = new File("target/plosone/0115884/");
		if (container0115884.exists()) FileUtils.forceDelete(container0115884);
		FileUtils.copyDirectory(Fixtures.F0115884_DIR, container0115884);
		String args = "-q "+container0115884.toString()+" --transform nlm2html --input fulltext.xml --output scholarly.html";
		Norma norma = new Norma();
		norma.run(args);
		CMDirList cmDirList = norma.getArgProcessor().getCMDirList();
		Assert.assertNotNull(cmDirList);
		Assert.assertEquals("CMDir/s",  1,  cmDirList.size());
		CMDir cmDir = cmDirList.get(0);
		List<File> files = cmDir.listFiles(true);
		LOG.trace(files);
		Assert.assertEquals(5, files.size());
	}
	
	/** normalizes an XML file and writes out shtml.
	 * 
	 * Not fully tagged. this is to test directory mechanism.
	 * 
	 * @throws IOException
	 */
	@Test
	@Ignore // FIXME 
	public void testCMDirWithDTD() throws IOException {
		File container0115884 = new File("target/plosone/0115884withdtd/");
		if (container0115884.exists()) FileUtils.forceDelete(container0115884);
		FileUtils.copyDirectory(Fixtures.F0115884_DIR, container0115884);
		String[] args = {
			"-q", container0115884.toString(), // output from quickscrape
			"--transform", "nlm2html",                  // stylesheet to use (code)
			"--standalone", "false",           // force use of DTD. May fail
			"-e", "xml"                       // type of file to transform
		};
		if (1==1) throw new RuntimeException("Recast as CMDir");

		int expectedFileCount = 5; // because of the output file
		
		// note the XML file has a DTD and takes 10 secs to process because of repeated downloads.
		DefaultArgProcessor argProcessor = new NormaArgProcessor(args);
		try {
//			argProcessor.normalizeAndTransform();
		} catch (Exception e) {
			// not connected
			String errorMessage = ExceptionUtils.getRootCauseMessage(e);
			Assert.assertEquals("UnknownHostException: dtd.nlm.nih.gov", errorMessage.trim());
			expectedFileCount = 4; // no output file
		}
		CMDirList cmDirList = argProcessor.getCMDirList();
		Assert.assertNotNull(cmDirList);
		Assert.assertEquals("CMDir/s",  1,  cmDirList.size());
		CMDir cmDir = cmDirList.get(0);
		List<File> files = cmDir.listFiles(true);
		Assert.assertEquals(expectedFileCount, files.size());
	}
	
	@Test
	/** transforms PDF to text
	 * 
	 *  // SHOWCASE
	 * 
	 * @throws IOException
	 */
	public void testPDF2TXT() throws IOException {
		File container0115884 = new File("target/plosone/0115884/");
		if (container0115884.exists()) FileUtils.forceDelete(container0115884);
		FileUtils.copyDirectory(Fixtures.F0115884_DIR, container0115884);
		String args = "-q "+container0115884.toString()+" --transform pdf2txt --input fulltext.pdf --output fulltext.pdf.txt";
		LOG.trace(args);
		Norma norma = new Norma();
		norma.run(args);
		CMDirList cmDirList = norma.getArgProcessor().getCMDirList();
		Assert.assertNotNull(cmDirList);
		Assert.assertEquals("CMDir/s",  1,  cmDirList.size());
		CMDir cmDir = cmDirList.get(0);
		List<File> files = cmDir.listFiles(true);
		LOG.trace(files);
		Assert.assertEquals(5, files.size());
	}
	
	@Test
	public void testPubstyle() throws Exception {
		String args = "--pubstyle bmc";
		Norma norma = new Norma();
		norma.run(args);
		DefaultArgProcessor argProcessor = (DefaultArgProcessor) norma.getArgProcessor();
		List<ArgumentOption> chosenOptions = argProcessor.getChosenArgumentList();
	}

	@Test
	public void testMultipleCMDirs() throws IOException {
		File containerPLOSONE = new File("target/plosone/multiple/");
		if (containerPLOSONE.exists()) FileUtils.forceDelete(containerPLOSONE);
		FileUtils.copyDirectory(new File(Fixtures.TEST_PUBSTYLE_DIR, "plosoneMultiple"), containerPLOSONE);
		String args = "-q "+containerPLOSONE.toString()+" --transform nlm2html --input fulltext.xml --output scholarly.html";
		LOG.trace("args> "+args);
		Norma norma = new Norma();
		norma.run(args);
	}
	
	@Test
	/** creates new CMDirs for list of PDF and then transforms
	 * 
	 */
	public void testNormalizeIEEEPDFs() throws IOException {
		String args;
		args = "-i fulltext.pdf --cmdir ../cmine/target/ieee/musti/Henniger -o fulltext.txt --transform pdf2txt";
		new Norma().run(args);
	}

}
