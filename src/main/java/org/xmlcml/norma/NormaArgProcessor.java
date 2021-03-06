package org.xmlcml.norma;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.imageio.ImageIO;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import nu.xom.Builder;
import nu.xom.Element;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.xmlcml.cmine.args.ArgIterator;
import org.xmlcml.cmine.args.ArgumentOption;
import org.xmlcml.cmine.args.DefaultArgProcessor;
import org.xmlcml.cmine.args.StringPair;
import org.xmlcml.cmine.files.CMDir;
import org.xmlcml.xml.XMLUtil;

/** 
 * Processes commandline arguments.
 * for Norma
 * 
 * @author pm286
 */
public class NormaArgProcessor extends DefaultArgProcessor{
	
	private static final String DOT_PNG = ".png";
	private static final String IMAGE = "image";
	private static final String PNG = "png";
	public static final Logger LOG = Logger.getLogger(NormaArgProcessor.class);
	static {
		LOG.setLevel(Level.DEBUG);
	}
	
	private static final String STYLESHEET_BY_NAME_XML = "/org/xmlcml/norma/pubstyle/stylesheetByName.xml";
	private static final String NAME = "name";
	private static final String XML = "xml";
	
	public final static String HELP_NORMA = "Norma help";
	
	private static String RESOURCE_NAME_TOP = "/org/xmlcml/norma";
	private static String ARGS_RESOURCE = RESOURCE_NAME_TOP+"/"+"args.xml";
	
	public final static String DOCTYPE = "!DOCTYPE";
	private static final List<String> TRANSFORM_OPTIONS = Arrays.asList(new String[]{
			"pdfbox", "pdf2html", "pdf2txt", "pdf2images",
			"hocr2svg"});
	// options
	private List<StringPair> charPairList;
	private List<String> divList;
	private List<StringPair> namePairList;
	private Pubstyle pubstyle;
	private List<String> stripList;
	private String tidyName;
	private List<String> xslNameList;
	private Map<String, String> stylesheetByNameMap;
	private boolean standalone;
	private List<String> transformList;
	private List<org.w3c.dom.Document> xslDocumentList;
	private NormaTransformer normaTransformer;

	public NormaArgProcessor() {
		super();
		this.readArgumentOptions(ARGS_RESOURCE);
	}

	public NormaArgProcessor(String[] args) {
		this();
		parseArgs(args);
	}

	// ============= METHODS =============
	
 	public void parseChars(ArgumentOption option, ArgIterator argIterator) {
		List<String> tokens = argIterator.createTokenListUpToNextNonDigitMinus(option);
		charPairList = option.processArgs(tokens).getStringPairValues();
	}

	public void parseDivs(ArgumentOption option, ArgIterator argIterator) {
		divList = argIterator.createTokenListUpToNextNonDigitMinus(option);
	}

	public void parseNames(ArgumentOption option, ArgIterator argIterator) {
		List<String> tokens = argIterator.createTokenListUpToNextNonDigitMinus(option);
		namePairList = option.processArgs(tokens).getStringPairValues();
	}
	
	public void parsePubstyle(ArgumentOption option, ArgIterator argIterator) {
		List<String> tokens = argIterator.createTokenListUpToNextNonDigitMinus(option);
		if (tokens.size() == 0) {
			stripList = new ArrayList<String>();
			Pubstyle.help();
		} else {
			String name = option.processArgs(tokens).getStringValue();
			pubstyle = Pubstyle.getPubstyle(name);
		}
	}

	public void parseStandalone(ArgumentOption option, ArgIterator argIterator) {
		List<String> tokens = argIterator.createTokenListUpToNextNonDigitMinus(option);
		try {
			standalone = tokens.size() == 1 ? new Boolean(tokens.get(0)) : false;
		} catch (Exception e) {
			throw new RuntimeException("bad boolean: "+tokens.get(0));
		}
	}

	public void parseStrip(ArgumentOption option, ArgIterator argIterator) {
		List<String> tokens = argIterator.createTokenListUpToNextNonDigitMinus(option);
		if (tokens.size() == 0) {
			stripList = new ArrayList<String>();
		} else {
			stripList = tokens;
		}
	}

	public void parseTidy(ArgumentOption option, ArgIterator argIterator) {
		List<String> tokens = argIterator.createTokenListUpToNextNonDigitMinus(option);
		tidyName = option.processArgs(tokens).getStringValue();
	}

	/** deprecated // use transform instead
	 * 
	 * @param option
	 * @param argIterator
	 */
	public void parseXsl(ArgumentOption option, ArgIterator argIterator) {
		LOG.warn("option --xsl is deprecated); use --transform instead");
		List<String> tokens = argIterator.createTokenListUpToNextNonDigitMinus(option);
		List<String> tokenList = option.processArgs(tokens).getStringValues();
		xslDocumentList = new ArrayList<org.w3c.dom.Document>();
		transformList = new ArrayList<String>();
		// at present we allow only one option
		for (String token : tokenList) {
			org.w3c.dom.Document xslDocument = createW3CStylesheetDocument(token);
			if (xslDocument != null) {
				xslDocumentList.add(xslDocument);
			} else if (TRANSFORM_OPTIONS.contains(token)) {
				transformList.add(token);
			} else {
				LOG.error("Cannot process transform token: "+token);
			}
		}
	}

	/** deprecated
	 * 
	 * @param option
	 * @param argIterator
	 */
	public void parseTransform(ArgumentOption option, ArgIterator argIterator) {
		List<String> tokens = argIterator.createTokenListUpToNextNonDigitMinus(option);
		List<String> tokenList = option.processArgs(tokens).getStringValues();
		xslDocumentList = new ArrayList<org.w3c.dom.Document>();
		transformList = new ArrayList<String>();
		// at present we allow only one option
		for (String token : tokenList) {
			org.w3c.dom.Document xslDocument = createW3CStylesheetDocument(token);
			if (xslDocument != null) {
				xslDocumentList.add(xslDocument);
			} else if (TRANSFORM_OPTIONS.contains(token)) {
				transformList.add(token);
			} else {
				LOG.error("Cannot process transform token: "+token);
			}
		}
	}

	public void printHelp() {
		System.err.println(
				"\n"
				+ "====NORMA====\n"
				+ "Norma converters raw files into scholarlyHTML, and adds tags to sections.\n"
				+ "Some of the conversion is dependent on publication type (--pubstyle) while some\n"
				+ "is constant for all documents. Where possible Norma guesses the input type, but can\n"
				+ "also be guided with the --extensions flag where the file/URL has no extension. "
				+ ""
				);
		super.printHelp();
	}
	
	// ===========run===============
	
	public void transform(ArgumentOption option) {
		// deprecated so
		runTransform(option);
	}

	public void runTransform(ArgumentOption option) {
		LOG.trace("***run transform "+currentCMDir);
		NormaTransformer normaTransformer = getOrCreateNormaTransformer();
		normaTransformer.transform(option);
	}

	private NormaTransformer getOrCreateNormaTransformer() {
		if (normaTransformer == null) {
			normaTransformer = new NormaTransformer(this);
		}
		return normaTransformer;
	}

	public void runTest(ArgumentOption option) {
		LOG.debug("RUN_TEST "+"is a dummy");
	}
		

	// =============output=============
	public void outputMethod(ArgumentOption option) {
		outputSpecifiedFormat();
	}

	private void outputSpecifiedFormat() {
		getOrCreateNormaTransformer();
		if (normaTransformer.outputTxt != null) {
			currentCMDir.writeFile(normaTransformer.outputTxt, (output != null ? output : CMDir.FULLTEXT_PDF_TXT));
		}
		if (normaTransformer.htmlElement != null) {
			currentCMDir.writeFile(normaTransformer.htmlElement.toXML(), (output != null ? output : CMDir.FULLTEXT_HTML));
		}
		if (normaTransformer.xmlStringList != null && normaTransformer.xmlStringList.size() > 0) {
			currentCMDir.writeFile(normaTransformer.xmlStringList.get(0), (output != null ? output : CMDir.SCHOLARLY_HTML));
		}
		if (normaTransformer.svgElement != null && output != null) {
			currentCMDir.writeFile(normaTransformer.svgElement.toXML(), output);
		}
		if (normaTransformer.serialImageList != null) {
			writeImages();
		}
	}

	private void writeImages() {
		File imageDir = currentCMDir.getOrCreateExistingImageDir();
		Set<String> imageSerialSet = new HashSet<String>();
		StringBuilder sb = new StringBuilder();
		for (Pair<String, BufferedImage> serialImage : normaTransformer.serialImageList) {
			try {
				String serialString = serialImage.getLeft();
				String imageSerial = serialString.split("\\.")[3];
				sb.append(serialString);
				if (!imageSerialSet.contains(imageSerial)) {
					File imageFile = new File(imageDir, serialString+DOT_PNG);
					ImageIO.write(serialImage.getRight(), PNG, imageFile);
					imageSerialSet.add(imageSerial);
				}
			} catch (IOException e) {
				throw new RuntimeException("Cannot write image ", e);
			}
		}
	}

	// ==========================
	
	private void ensureXslDocumentList() {
		if (xslDocumentList == null) {
			xslDocumentList = new ArrayList<org.w3c.dom.Document>();
		}
	}

	File checkAndGetInputFile(CMDir cmDir) {
		String inputName = getString();
		if (inputName == null) {
			throw new RuntimeException("Must have single input option");
		}
		if (!CMDir.isReservedFilename(inputName) && !CMDir.hasReservedParentDirectory(inputName) ) {
			throw new RuntimeException("Input must be reserved file; found: "+inputName);
		}
		File inputFile = cmDir.getExistingReservedFile(inputName);
		if (inputFile == null) {
			inputFile = cmDir.getExistingFileWithReservedParentDirectory(inputName);
		}
		if (inputFile == null) {
			throw new RuntimeException("Could not find input file "+inputName+" in directory "+cmDir.getDirectory());
		}
		return inputFile;
	}

	private void createCMDirListFromInputList() {
		// proceed unless there is a single reserved file for input
		if (CMDir.isNonEmptyNonReservedInputList(inputList)) {
			LOG.trace("CREATING CMDir FROM INPUT:"+inputList);
			// this actually creates directory
			getOrCreateOutputDirectory();
			ensureCMDirList();
			createNewCMDirsAndCopyOriginalFilesAndAddToList();
		}
	}

	private void createNewCMDirsAndCopyOriginalFilesAndAddToList() {
		ensureCMDirList();
		for (String filename : inputList) {
			try {
				CMDir cmDir = createCMDirAndCopyFileOrMakeSubDirectory(filename);
				if (cmDir != null) {
					cmDirList.add(cmDir);
				}
			} catch (IOException e) {
				LOG.error("Failed to create CMDir: "+filename+"; "+e);
			}
		}
	}

	private CMDir createCMDirAndCopyFileOrMakeSubDirectory(String filename) throws IOException {
		CMDir cmDir = null;
		File file = new File(filename);
		if (file.isDirectory()) {
			LOG.error("should not have any directories in inputList: "+file);
		} else {
			if (output != null) {
				String name = FilenameUtils.getName(filename);
				if (CMDir.isReservedFilename(name)) {
					LOG.error(name+" is reserved for CMDir: (check that inputs are not already in a CMDir) "+file.getAbsolutePath());
				}
				String cmFilename = CMDir.getCMDirReservedFilenameForExtension(name);
				if (cmFilename == null) {
					LOG.error("Cannot create CMDir from this type of file: "+name);
					return null;
				}
				LOG.trace("Reserved filename: "+cmFilename);
				if (CMDir.isReservedDirectory(cmFilename)) {
					cmDir = makeCMDir(name);
					ensureReservedDirectoryAndCopyFile(cmDir, cmFilename, filename);
				} else {
					cmDir = makeCMDir(name);
					File destFile = cmDir.getReservedFile(cmFilename);
					if (destFile != null) {
						FileUtils.copyFile(file, destFile);
					}
				}
			}
		}
		return cmDir;
	}

	private CMDir makeCMDir(String name) {
		CMDir cmDir;
		String dirName = FilenameUtils.removeExtension(name);
		cmDir = createCMDir(dirName);
		return cmDir;
	}

	private void ensureReservedDirectoryAndCopyFile(CMDir cmDir, String reservedFilename, String filename) {
		File reservedDir = new File(cmDir.getDirectory(), reservedFilename);
		LOG.trace("Res "+reservedDir.getAbsolutePath());
		File orig = new File(filename);
		LOG.trace("Orig: "+orig.getAbsolutePath());
		try {
			FileUtils.forceMkdir(reservedDir);
		} catch (IOException e) {
			throw new RuntimeException("Cannot make directory: "+reservedFilename+" "+e);
		}  
		String name = FilenameUtils.getName(filename);
		try {
			File outFile = new File(reservedDir, name);
			FileUtils.copyFile(new File(filename), outFile);
		} catch (IOException e) {
			throw new RuntimeException("Cannot copy file: "+filename+" to "+reservedDir+" / "+e);
		}  
		LOG.debug("created "+name+" in "+reservedDir);
		
	}

	private CMDir createCMDir(String dirName) {
		File cmDirFile = new File(output, dirName);
		CMDir cmDir = new CMDir(cmDirFile);
		cmDir.createDirectory(cmDirFile, false);
		return cmDir;
	}

	private void getOrCreateOutputDirectory() {
		if (output != null) {
			File outputDir = new File(output);
			if (outputDir.exists()) {
				if (!outputDir.isDirectory()) {
					throw new RuntimeException("cmDirRoot "+outputDir+" must be a directory");
				}
			} else {
				outputDir.mkdirs();
			}
		}
	}

	private org.w3c.dom.Document createW3CStylesheetDocument(String xslName) {
		DocumentBuilder db = createDocumentBuilder(); 
		String stylesheetResourceName = replaceCodeIfPossible(xslName);
		org.w3c.dom.Document stylesheetDocument = readAsResource(db, stylesheetResourceName);
		// if fails, try as file
		if (stylesheetDocument == null) {
			try {
				stylesheetDocument = readAsStream(db, xslName, new FileInputStream(xslName));
			} catch (FileNotFoundException e) { /* hide exception*/}
		}
		if (stylesheetDocument == null) {
			// this could happen when we use "pdf2txt" , etc
			LOG.trace("Cannot read stylesheet: "+xslName+"; "+stylesheetResourceName);
		}
		return stylesheetDocument;
	}

	private DocumentBuilder createDocumentBuilder() {
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder db = null;
		try {
			db = dbf.newDocumentBuilder();
		} catch (ParserConfigurationException e) {
			throw new RuntimeException("Serious BUG in JavaXML:", e);
		}
		return db;
	}

	private org.w3c.dom.Document readAsResource(DocumentBuilder db, String stylesheetResourceName) {
		InputStream is = this.getClass().getResourceAsStream(stylesheetResourceName);
		return readAsStream(db, stylesheetResourceName, is);
	}

	private org.w3c.dom.Document readAsStream(DocumentBuilder db, String xslName, InputStream is) {
		org.w3c.dom.Document stylesheetDocument = null;
		try {
			stylesheetDocument = db.parse(is);
		} catch (Exception e) { /* hide error */ }
		return stylesheetDocument;
	}

	private String replaceCodeIfPossible(String xslName) {
		createStylesheetByNameMap();
		String stylesheetResourceName = stylesheetByNameMap.get(xslName);
		stylesheetResourceName = (stylesheetResourceName == null) ? xslName : stylesheetResourceName;
		return stylesheetResourceName;
	}

	private void createStylesheetByNameMap() {
		stylesheetByNameMap = new HashMap<String, String>();
		try {
			nu.xom.Document stylesheetByNameDocument = new Builder().build(this.getClass().getResourceAsStream(STYLESHEET_BY_NAME_XML));
			List<Element> stylesheetList = XMLUtil.getQueryElements(stylesheetByNameDocument, "/stylesheetList/stylesheet");
			for (Element stylesheet : stylesheetList) {
				stylesheetByNameMap.put(stylesheet.getAttributeValue(NAME), stylesheet.getValue());
			}
			LOG.trace(stylesheetByNameMap);
		} catch (Exception e) {
			LOG.error("Cannot read "+STYLESHEET_BY_NAME_XML+"; "+e);
		}
	}



	// ==========================

	public Pubstyle getPubstyle() {
		return pubstyle;
	}

	public List<String> getStylesheetNameList() {
		return xslNameList;
	}

	public List<StringPair> getCharPairList() {
		return charPairList;
	}

	public List<String> getDivList() {
		return divList;
	}

	public List<StringPair> getNamePairList() {
		return namePairList;
	}

	public List<String> getStripList() {
		return stripList;
	}

	public String getTidyName() {
		return tidyName;
	}

	public boolean isStandalone() {
		return standalone;
	}
	
	@Override
	/** parse args and resolve their dependencies.
	 * 
	 * (don't run any argument actions)
	 * 
	 */
	public void parseArgs(String[] args) {
		super.parseArgs(args);
		createCMDirListFromInputList();
	}

	public CMDir getCurrentCMDir() {
		return currentCMDir;
	}

	public List<Document> getXslDocumentList() {
		ensureXslDocumentList();
		return xslDocumentList;
	}


}
