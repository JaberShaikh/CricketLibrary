package com.cricket.model;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name="Configurations")
@XmlAccessorType(XmlAccessType.FIELD)
public class Configuration {
	
	private static Configuration currentConfig;
	
	@XmlElement(name="filename")
	private String filename;

	@XmlElement(name="broadcaster")
	private String broadcaster;

	@XmlElement(name="primaryIpAddress")
	private String primaryIpAddress;
	
	@XmlElement(name="primaryPortNumber")
	private int primaryPortNumber;
	
	@XmlElement(name="primaryLanguage")
	private String primaryLanguage;

	@XmlElement(name="speedUnit")
	private String speedUnit;
	
	@XmlElement(name="preview")
	private String preview;
	
	@XmlElement(name="showSpeed")
	private String showSpeed;
	
	@XmlElement(name="generateInteractiveFile")
	private String generateInteractiveFile;
	
	@XmlElement(name="cricketDirectory")
	private String cricketDirectory;
	
	public Configuration() {
		super();
	}

	public Configuration(String filename, String broadcaster, String primaryIpAddress, int primaryPortNumber,
			String primaryLanguage, String speedUnit, String preview, String showSpeed, String generateInteractiveFile,
			String cricketDirectory) {
		super();
		this.filename = filename;
		this.broadcaster = broadcaster;
		this.primaryIpAddress = primaryIpAddress;
		this.primaryPortNumber = primaryPortNumber;
		this.primaryLanguage = primaryLanguage;
		this.speedUnit = speedUnit;
		this.preview = preview;
		this.showSpeed = showSpeed;
		this.generateInteractiveFile = generateInteractiveFile;
		this.cricketDirectory = cricketDirectory;
	}
	
	public static Configuration getCurrentConfig() {
		return currentConfig;
	}

	public static void setCurrentConfig(Configuration currentConfig) {
		Configuration.currentConfig = currentConfig;
	}

	public String getFilename() {
		return filename;
	}

	public void setFilename(String filename) {
		this.filename = filename;
	}

	public String getBroadcaster() {
		return broadcaster;
	}

	public void setBroadcaster(String broadcaster) {
		this.broadcaster = broadcaster;
	}

	public String getPrimaryIpAddress() {
		return primaryIpAddress;
	}

	public void setPrimaryIpAddress(String primaryIpAddress) {
		this.primaryIpAddress = primaryIpAddress;
	}

	public int getPrimaryPortNumber() {
		return primaryPortNumber;
	}

	public void setPrimaryPortNumber(int primaryPortNumber) {
		this.primaryPortNumber = primaryPortNumber;
	}

	public String getPrimaryLanguage() {
		return primaryLanguage;
	}

	public void setPrimaryLanguage(String primaryLanguage) {
		this.primaryLanguage = primaryLanguage;
	}

	public String getSpeedUnit() {
		return speedUnit;
	}

	public void setSpeedUnit(String speedUnit) {
		this.speedUnit = speedUnit;
	}

	public String getPreview() {
		return preview;
	}

	public void setPreview(String preview) {
		this.preview = preview;
	}

	public String getShowSpeed() {
		return showSpeed;
	}

	public void setShowSpeed(String showSpeed) {
		this.showSpeed = showSpeed;
	}

	public String getGenerateInteractiveFile() {
		return generateInteractiveFile;
	}

	public void setGenerateInteractiveFile(String generateInteractiveFile) {
		this.generateInteractiveFile = generateInteractiveFile;
	}

	public String getCricketDirectory() {
		return cricketDirectory;
	}

	public void setCricketDirectory(String cricketDirectory) {
		this.cricketDirectory = cricketDirectory;
	}

	@Override
	public String toString() {
		return "Configuration [filename=" + filename + ", broadcaster=" + broadcaster + ", primaryIpAddress="
				+ primaryIpAddress + ", primaryPortNumber=" + primaryPortNumber + ", primaryLanguage=" + primaryLanguage
				+ ", speedUnit=" + speedUnit + ", preview=" + preview + ", showSpeed=" + showSpeed
				+ ", generateInteractiveFile=" + generateInteractiveFile + ", cricketDirectory=" + cricketDirectory
				+ "]";
	}
}
