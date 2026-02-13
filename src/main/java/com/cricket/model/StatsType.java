package com.cricket.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.persistence.Column;

@Entity
@Table(name = "StatsType")
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonIgnoreProperties(ignoreUnknown = true)
public class StatsType {

  @Id
  @Column(name = "STATSID")
  private int stats_id;
	
  @Column(name = "STATSFULLNAME")
  private String stats_full_name;

  @Column(name = "STATSSHORTNAME")
  private String stats_short_name;

public int getStats_id() {
	return stats_id;
}

public void setStats_id(int stats_id) {
	this.stats_id = stats_id;
}

public String getStats_full_name() {
	return stats_full_name;
}

public void setStats_full_name(String stats_full_name) {
	this.stats_full_name = stats_full_name;
}

public String getStats_short_name() {
	return stats_short_name;
}

public void setStats_short_name(String stats_short_name) {
	this.stats_short_name = stats_short_name;
}

@Override
public String toString() {
	return "StatsType [stats_id=" + stats_id + ", stats_full_name=" + stats_full_name + ", stats_short_name="
			+ stats_short_name + "]";
}

}