package com.cricket.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.persistence.Column;

@Entity
@Table(name = "FantasyImages")
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonIgnoreProperties(ignoreUnknown = true)
public class FantasyImages
{
  @Id
  @Column(name = "FantasyID")
  private int fantasyId;
  
  @Column(name = "Prompt")
  private String prompt;

  @Column(name = "ImageName")
  private String imagename;
    
  public FantasyImages() {
		super();
  }

public FantasyImages(int fantasyId) {
	super();
	this.fantasyId = fantasyId;
}

public int getFantasyId() {
	return fantasyId;
}

public void setFantasyId(int fantasyId) {
	this.fantasyId = fantasyId;
}

public String getPrompt() {
	return prompt;
}

public void setPrompt(String prompt) {
	this.prompt = prompt;
}

public String getImagename() {
	return imagename;
}

public void setImagename(String imagename) {
	this.imagename = imagename;
}


}