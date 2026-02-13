package com.cricket.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.persistence.Column;

@Entity
@Table(name = "Commentators")
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonIgnoreProperties(ignoreUnknown = true)
public class Commentator {

  @Id
  @Column(name = "CommentatorId")
  private int commentatorId;
	
  @Column(name = "CommentatorName")
  private String commentatorName;
  
  @Column(name = "FirstName")
  private String firstName;
  
  @Column(name = "LastName")
  private String lastName;
  
  @Column(name = "PhotoName")
  private String photoName;

  @Column(name = "UseThis")
  private String useThis;

public int getCommentatorId() {
	return commentatorId;
}

public void setCommentatorId(int commentatorId) {
	this.commentatorId = commentatorId;
}

public String getCommentatorName() {
	return commentatorName;
}

public void setCommentatorName(String commentatorName) {
	this.commentatorName = commentatorName;
}

public String getUseThis() {
	return useThis;
}

public void setUseThis(String useThis) {
	this.useThis = useThis;
}

public String getFirstName() {
	return firstName;
}

public void setFirstName(String firstName) {
	this.firstName = firstName;
}

public String getLastName() {
	return lastName;
}

public void setLastName(String lastName) {
	this.lastName = lastName;
}

public String getPhotoName() {
	return photoName;
}

public void setPhotoName(String photoName) {
	this.photoName = photoName;
}
  
}