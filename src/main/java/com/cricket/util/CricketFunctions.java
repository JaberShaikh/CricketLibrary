package com.cricket.util;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import com.cricket.model.*;
import com.cricket.service.CricketService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.exc.StreamWriteException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DatabindException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import jakarta.xml.bind.JAXBException;

public class CricketFunctions {
	
	public static ObjectWriter objectWriter = new ObjectMapper().writer().withDefaultPrettyPrinter();
	private static ObjectMapper objectMapper = new ObjectMapper();
	
	public static Speed getCurrentSpeed(String speedFilePath, Speed lastSpeed) throws IOException {
		if(!speedFilePath.trim().isEmpty()) {
			File speed_file = new File(speedFilePath);
			if(speed_file.exists()) {
				if(lastSpeed.getSpeedFileModifiedTime() != speed_file.lastModified()) {
					return new Speed(Files.readAllLines(Paths.get(speedFilePath), StandardCharsets.UTF_8).get(0), 
							speed_file.lastModified());
				}
			}
		}
		return null;
	}	
	
	public static List<BowlingCard> MergeMissingSpeeds(int inningNumber, List<BowlingCard> bowlingCards, List<Speed> allSpeeds) {

	    for (BowlingCard bc : bowlingCards) {

	        if (bc.getSpeeds() == null || bc.getSpeeds().isEmpty())
	            continue;

	        List<Speed> originalSpeeds = objectMapper.convertValue(bc.getSpeeds(), new TypeReference<List<Speed>>() {});

	        List<Speed> newSpeeds = new ArrayList<>();

	        Set<Integer> inningOversSet = new HashSet<>();
	        for (Speed s : originalSpeeds) {
	            inningOversSet.add(s.getInningTotalOver());
	        }

	        List<Integer> inningOvers = new ArrayList<>(inningOversSet);
	        Collections.sort(inningOvers);

	        for (Integer inningOver : inningOvers) {

	            int effectiveOver = inningOver + 1;

	            List<Speed> matchSpeeds = allSpeeds.stream().filter(s -> s.getInningNumber() == inningNumber 
	            	&& s.getOverNumber() == effectiveOver).collect(Collectors.toList());

	            List<Speed> originalForOver = originalSpeeds.stream().filter(s -> s.getInningTotalOver() == inningOver).collect(Collectors.toList());

	            if (!matchSpeeds.isEmpty()) {

	                int speedCount = 0;

	                for (int i = 0; i < matchSpeeds.size(); i++) {

	                    Speed m = matchSpeeds.get(i);

	                    Speed context = i < originalForOver.size() ? originalForOver.get(i) : originalForOver.get(originalForOver.size() - 1);

	                    speedCount++;

	                    newSpeeds.add(new Speed(speedCount, m.getSpeedValue(), "", context.getOverNumber(), context.getBallNumber(), 
	                    	context.getInningTotalOver(), context.getInningTotalBall(), inningNumber));
	                }

	            } else {
	                newSpeeds.addAll(originalForOver);
	            }
	        }
	        bc.setSpeeds(newSpeeds);
	    }

	    return bowlingCards;
	}	
	
	public static List<Speed> ReadBallSpeedData(String filePath) throws Exception {

	    return Files.lines(Paths.get(filePath))
            .filter(line -> !line.trim().isEmpty())
            .map(line -> {
                Map<String, String> values = Arrays.stream(line.split(","))
                        .map(part -> part.split("=", 2))
                        .collect(Collectors.toMap(
                                a -> a[0].trim(),
                                a -> a[1].trim()
                        ));

                return new Speed(Integer.parseInt(values.get("Inning")), values.get("Speed"), 
                	Integer.parseInt(values.get("Over")), Integer.parseInt(values.get("Ball")));
            })
            .collect(Collectors.toList());
	}
	
	public static Match processInningTimeData(String whatToProcess, Match matchData, String timeStatsToProcess, Match lastMatchData) 
	{
		if(matchData != null && matchData.getInning() != null && matchData.getInning().size() > 0)
		{
			for (Inning this_inn : matchData.getInning()) {
				
				if(this_inn.getIsCurrentInning().equalsIgnoreCase(CricketUtil.YES) && this_inn.getInningStatus().equalsIgnoreCase(CricketUtil.START)) {
					
					switch (whatToProcess) {
					case "PROCESS_TIME_STATS":
						
						if(!timeStatsToProcess.isEmpty() && timeStatsToProcess.split(",").length >= 4) {
							
							this_inn.setDuration(Integer.valueOf(timeStatsToProcess.split(",")[0]));
							if(matchData.getDaysSessions() != null) {
								for(DaySession ds : matchData.getDaysSessions()) {
									if(ds.getIsCurrentSession().equalsIgnoreCase(CricketUtil.YES)) {
										ds.setTotalSeconds(Integer.valueOf(timeStatsToProcess.split(",")[0]));
									}
								}
							}		
							
							if(this_inn.getInningStats() == null) {
								this_inn.setInningStats(new InningStats());
							}
							if(this_inn.getInningStats().getBallsPerHour() == null) {
								this_inn.getInningStats().setBallsPerHour(new ArrayList<Integer>());
							}

							if(this_inn.getDuration() / 3600 > this_inn.getInningStats().getBallsPerHour().size()) {
								this_inn.getInningStats().getBallsPerHour().add(6 * this_inn.getTotalOvers() + this_inn.getTotalBalls());
							}

							this_inn.getInningStats().setTimeSinceLastBoundary(Integer.valueOf(timeStatsToProcess.split(",")[1]));
							this_inn.getInningStats().setTimeSinceLastRun(Integer.valueOf(timeStatsToProcess.split(",")[2]));
							this_inn.getInningStats().setTimeSinceLastRunOffBat(Integer.valueOf(timeStatsToProcess.split(",")[3]));
							
							if(timeStatsToProcess.split(",").length >= 5) {
								if(this_inn.getBattingCard() != null && this_inn.getBattingCard().size() > 0) {
									List<BattingCard> this_bcs = this_inn.getBattingCard().stream().filter(
										bc -> bc.getStatus().equalsIgnoreCase(CricketUtil.NOT_OUT)).collect(Collectors.toList());
									for (BattingCard bc : this_bcs) {
										for(int bc_index = 4; bc_index <= timeStatsToProcess.split(",").length-1; bc_index++) {
											if(timeStatsToProcess.split(",")[bc_index].contains(String.valueOf(bc.getPlayerId()) + "_")) {
												bc.setDuration(Integer.valueOf(timeStatsToProcess.split(",")[bc_index].split("_")[1]));
											}
										}
									}
								}
							}
							
						}
						if(lastMatchData != null && lastMatchData.getInning() != null && lastMatchData.getInning().size() > 0)
						{
							Inning last_inn = lastMatchData.getInning().stream().filter(
								inn -> inn.getIsCurrentInning().equalsIgnoreCase(CricketUtil.YES) 
								&& inn.getInningStatus().equalsIgnoreCase(CricketUtil.START)).findAny().orElse(null);

							if(last_inn != null) {
								if(last_inn.getInningStats() == null) {
									last_inn.setInningStats(new InningStats());
								}
								if(last_inn.getTotalRuns() != this_inn.getTotalRuns()) {
									this_inn.getInningStats().setTimeSinceLastRun(0);
								}
								if(last_inn.getTotalFours() != this_inn.getTotalFours()
									|| last_inn.getTotalSixes() != this_inn.getTotalSixes()) {
									this_inn.getInningStats().setTimeSinceLastBoundary(0);
								}
								if(last_inn.getBattingCard() != null && this_inn.getBattingCard() != null
									&& last_inn.getBattingCard().size() > 0 && this_inn.getBattingCard().size() > 0) {
									for (BattingCard last_bc : last_inn.getBattingCard()) {
										if(last_bc.getStatus().equalsIgnoreCase(CricketUtil.NOT_OUT)) {
											for (BattingCard this_bc : this_inn.getBattingCard()) {
												if(this_bc.getPlayerId() == last_bc.getPlayerId() 
													&& this_bc.getRuns() != last_bc.getRuns()) {
													this_inn.getInningStats().setTimeSinceLastRunOffBat(0);
												}
											}
										}
									}
								}
							}
						}
						break;
					}
				}
			}
		}
		return matchData;
	}
	
	public static String Plural(int count){
		if (count == 1){
			return "";
		} else{
			return "s";
		}
	}
	
	public static int GetTeamRunsAhead(int inning_number, MatchAllData matchAllData)
	{
		int teamTotalRuns = 0;
		if(inning_number > 0 && inning_number > matchAllData.getMatch().getInning().size()) {
			return teamTotalRuns;
		} else {
		    for(int inn = 1; inn <= inning_number; inn++) {
		    	if(matchAllData.getMatch().getInning().get(inn - 1).getBattingTeamId() 
		    		== matchAllData.getMatch().getInning().get(inning_number - 1).getBattingTeamId()) 
		    	{
					if(matchAllData.getSetup().getSpecialMatchRules() != null && matchAllData.getSetup().getSpecialMatchRules().equalsIgnoreCase(CricketUtil.ISPL)
						&& matchAllData.getMatch().getInning().get(inn - 1).getSpecialRuns() != null 
						&& !matchAllData.getMatch().getInning().get(inn - 1).getSpecialRuns().trim().isEmpty()) 
					{
						if(matchAllData.getMatch().getInning().get(inn - 1).getSpecialRuns().startsWith("+")) {
				    		teamTotalRuns = teamTotalRuns + matchAllData.getMatch().getInning().get(inn - 1).getTotalRuns()
				    			+ Integer.parseInt(matchAllData.getMatch().getInning().get(inn - 1).getSpecialRuns().replace("+", ""));
						}else if(matchAllData.getMatch().getInning().get(inn - 1).getSpecialRuns().startsWith("-")) {
				    		teamTotalRuns = teamTotalRuns + matchAllData.getMatch().getInning().get(inn - 1).getTotalRuns()
				    			- Integer.parseInt(matchAllData.getMatch().getInning().get(inn - 1).getSpecialRuns().replace("-", ""));
						}
					} else {
			    		teamTotalRuns = teamTotalRuns + matchAllData.getMatch().getInning().get(inn - 1).getTotalRuns();
					}
		    	}else {
					if(matchAllData.getSetup().getSpecialMatchRules() != null && matchAllData.getSetup().getSpecialMatchRules().equalsIgnoreCase(CricketUtil.ISPL)
						&& matchAllData.getMatch().getInning().get(inn - 1).getSpecialRuns() != null 
						&& !matchAllData.getMatch().getInning().get(inn - 1).getSpecialRuns().trim().isEmpty()) 
					{
						if(matchAllData.getMatch().getInning().get(inn - 1).getSpecialRuns().startsWith("+")) {
				    		teamTotalRuns = teamTotalRuns - matchAllData.getMatch().getInning().get(inn - 1).getTotalRuns()
				    			+ Integer.parseInt(matchAllData.getMatch().getInning().get(inn - 1).getSpecialRuns().replace("+", ""));
						}else if(matchAllData.getMatch().getInning().get(inn - 1).getSpecialRuns().startsWith("-")) {
				    		teamTotalRuns = teamTotalRuns - matchAllData.getMatch().getInning().get(inn - 1).getTotalRuns()
				    			- Integer.parseInt(matchAllData.getMatch().getInning().get(inn - 1).getSpecialRuns().replace("-", ""));
						}
					} else {
			    		teamTotalRuns = teamTotalRuns - matchAllData.getMatch().getInning().get(inn - 1).getTotalRuns();
					}
		    	}
		    }
		}
		return teamTotalRuns;
	}	
	
   public static TargetData GetTargetData(MatchAllData matchAllData) 
   {
	    TargetData targetData = new TargetData();
	   
	    if (matchAllData.getSetup().getTargetRuns() > 0) {
	    	targetData.setTargetRuns(matchAllData.getSetup().getTargetRuns());
	    }else {
	        if (matchAllData.getSetup().getMaxOvers() == 0) {
	        	targetData.setTargetRuns(GetTeamRunsAhead(3, matchAllData) + 1);
	        } else {
	        	targetData.setTargetRuns(GetTeamRunsAhead(1, matchAllData) + 1);
	        }
	    }
	    if (matchAllData.getSetup().getTargetOvers() != null && !matchAllData.getSetup().getTargetOvers().isEmpty()
	    	&& Integer.valueOf(matchAllData.getSetup().getTargetOvers().split("\\.")[0]) > 0) {
	    	targetData.setTargetOvers(matchAllData.getSetup().getTargetOvers());
	    } else {
	    	targetData.setTargetOvers(String.valueOf(matchAllData.getSetup().getMaxOvers()));
	    }
	    if (matchAllData.getSetup().getMaxOvers() > 0) {
	    	if(targetData.getTargetOvers().contains(".")) {
	    		targetData.setRemaningBall((Integer.valueOf(targetData.getTargetOvers().split("\\.")[0]) * 6 + 
	    			Integer.valueOf(targetData.getTargetOvers().split("\\.")[1])) - (matchAllData.getMatch().getInning().get(1).getTotalOvers() * 6)
			    		- matchAllData.getMatch().getInning().get(1).getTotalBalls());
	    	} else {
		    	targetData.setRemaningBall((Integer.valueOf(targetData.getTargetOvers()) * 6) - (matchAllData.getMatch().getInning().get(1).getTotalOvers() * 6)
		    		- matchAllData.getMatch().getInning().get(1).getTotalBalls());
	    	}
			if(matchAllData.getSetup().getSpecialMatchRules() != null && matchAllData.getSetup().getSpecialMatchRules().equalsIgnoreCase(CricketUtil.ISPL)
				&& matchAllData.getMatch().getInning().get(1).getSpecialRuns() != null && !matchAllData.getMatch().getInning().get(1).getSpecialRuns().trim().isEmpty()) 
			{
				if(matchAllData.getMatch().getInning().get(1).getSpecialRuns().startsWith("-")) {
			    	targetData.setRemaningRuns(targetData.getTargetRuns() - (matchAllData.getMatch().getInning().get(1).getTotalRuns() 
			    		- Integer.parseInt(matchAllData.getMatch().getInning().get(1).getSpecialRuns().replace("-", ""))));
				} else {
			    	targetData.setRemaningRuns(targetData.getTargetRuns() - (matchAllData.getMatch().getInning().get(1).getTotalRuns() 
			    		+ Integer.parseInt(matchAllData.getMatch().getInning().get(1).getSpecialRuns().replace("+", ""))));
				}
			} else {
		    	targetData.setRemaningRuns(targetData.getTargetRuns() - matchAllData.getMatch().getInning().get(1).getTotalRuns());
			}
	    } else {
	    	if (matchAllData.getSetup().getTargetOvers() != null && Integer.valueOf(matchAllData.getSetup().getTargetOvers()) > 0) {
		    	if(targetData.getTargetOvers().contains(".")) {
		    		targetData.setRemaningBall((Integer.valueOf(targetData.getTargetOvers().split("\\.")[0]) * 6 + 
		    			Integer.valueOf(targetData.getTargetOvers().split("\\.")[1])) - (matchAllData.getMatch().getInning().get(3).getTotalOvers() * 6)
				    		- matchAllData.getMatch().getInning().get(3).getTotalBalls());
		    	} else {
		    		targetData.setRemaningBall((Integer.valueOf(targetData.getTargetOvers()) * 6) - (matchAllData.getMatch().getInning().get(3).getTotalOvers() * 6)
			    		- matchAllData.getMatch().getInning().get(3).getTotalBalls());
		    	}
	    	} else {
	    		targetData.setRemaningBall(0);
	    	}
	    	targetData.setRemaningRuns(targetData.getTargetRuns() - matchAllData.getMatch().getInning().get(3).getTotalRuns());
	    }
	    if(targetData.getRemaningBall() < 0) {
	    	targetData.setRemaningBall(0);
	    } 
	    if(targetData.getRemaningRuns() < 0) {
	    	targetData.setRemaningRuns(0);
	    } 
	    return targetData;
	}
   
	public static String GenerateMatchResult(MatchAllData match, String teamNameType, String broadcaster, 
		String splitResultTxt, boolean ballsRemaining)
	{
		String resultToShow = "", opponentTeamName = "";
		
		if(match.getMatch().getMatchResult() != null) {
			if(match.getMatch().getMatchResult().toUpperCase().contains(CricketUtil.DRAWN)
					|| match.getMatch().getMatchResult().toUpperCase().contains(CricketUtil.ABANDONED)) {
				if(match.getMatch().getMatchResult().toUpperCase().contains(CricketUtil.DRAWN) 
						&& !match.getSetup().getMatchType().equalsIgnoreCase(CricketUtil.TEST)) { // For limited over match use match tied
					switch (broadcaster) {
					case "ICC-U19-2023": case "T20_MUMBAI":
						if(splitResultTxt.isEmpty()) {
							resultToShow = CricketUtil.MATCH.toLowerCase() + " " + CricketUtil.TIED.toLowerCase();
						} else {
							resultToShow = CricketUtil.MATCH.toLowerCase() + splitResultTxt + CricketUtil.TIED.toLowerCase();
						}
						break;
					default:
						resultToShow = CricketUtil.MATCH.toLowerCase() + " " + CricketUtil.TIED.toLowerCase();
						break;
					}
				} else {
					resultToShow = CricketUtil.MATCH.toLowerCase();
					if(match.getMatch().getMatchResult().toUpperCase().contains(CricketUtil.DRAWN)) {
						switch (broadcaster) {
						case "ICC-U19-2023": case "T20_MUMBAI":
							if(splitResultTxt.isEmpty()) {
								resultToShow = resultToShow + " " + CricketUtil.DRAWN.toLowerCase();
							} else {
								resultToShow = resultToShow + splitResultTxt + CricketUtil.DRAWN.toLowerCase();
							}
							break;
						default:
							resultToShow = resultToShow + " " + CricketUtil.DRAWN.toLowerCase();
							break;
						}
					} else if(match.getMatch().getMatchResult().toUpperCase().contains(CricketUtil.ABANDONED)) {
						switch (broadcaster) {
						case "ICC-U19-2023": case "T20_MUMBAI":
							if(splitResultTxt.isEmpty()) {
								resultToShow = resultToShow + " " + CricketUtil.ABANDONED.toLowerCase();
							} else {
								resultToShow = resultToShow + splitResultTxt + CricketUtil.ABANDONED.toLowerCase();
							}
							break;
						default:
							resultToShow = resultToShow + " " + CricketUtil.ABANDONED.toLowerCase();
							break;
						}
					}
				}
			} else if(match.getMatch().getMatchResult().toUpperCase().contains(CricketUtil.NO_RESULT)) {
				resultToShow = CricketUtil.NO_RESULT.toLowerCase().replace("_", " ");
			} else {
				if(match.getMatch().getMatchResult().contains(",")) {
					switch (teamNameType) {
					case CricketUtil.SHORT:
						if(Integer.valueOf(match.getMatch().getMatchResult().split(",")[0]) == match.getSetup().getHomeTeamId()) {
							resultToShow = match.getSetup().getHomeTeam().getTeamName4();
							opponentTeamName = match.getSetup().getAwayTeam().getTeamName4();
						} else {
							resultToShow = match.getSetup().getAwayTeam().getTeamName4();
							opponentTeamName = match.getSetup().getHomeTeam().getTeamName4();
						}
					    break;
					default:
						if(Integer.valueOf(match.getMatch().getMatchResult().split(",")[0]) == match.getSetup().getHomeTeamId()) {
							resultToShow = match.getSetup().getHomeTeam().getTeamName1();
							opponentTeamName = match.getSetup().getAwayTeam().getTeamName1();
						} else {
							resultToShow = match.getSetup().getAwayTeam().getTeamName1();
							opponentTeamName = match.getSetup().getHomeTeam().getTeamName1();
						}
					    break;
					}
					if(match.getMatch().getMatchResult().toUpperCase().contains(CricketUtil.SUPER_OVER)) {
						resultToShow = resultToShow + " win the super over";
					} else if(match.getMatch().getMatchResult().toUpperCase().contains(CricketUtil.INNING + "_LEAD")) {
						resultToShow = resultToShow + " win on first inning lead";
					} else if(match.getMatch().getMatchResult().toUpperCase().contains(CricketUtil.INNING) 
						&& match.getMatch().getMatchResult().toUpperCase().contains(CricketUtil.RUN)) {
						resultToShow = resultToShow + " win by an inning and " + Integer.valueOf(match.getMatch().getMatchResult().split(",")[1]) 
							+ " run" + Plural(Integer.valueOf(match.getMatch().getMatchResult().split(",")[1]));
					} else if (match.getMatch().getMatchResult().toUpperCase().contains(CricketUtil.RUN)) {
						switch (broadcaster) {
						case "ICC-U19-2023": case "T20_MUMBAI":
							if(splitResultTxt.isEmpty()) {
								resultToShow = resultToShow + " beat " + opponentTeamName + " by " + Integer.valueOf(match.getMatch().getMatchResult().split(",")[1]) 
									+ " run" + Plural(Integer.valueOf(match.getMatch().getMatchResult().split(",")[1]));
							} else {
								resultToShow = resultToShow + " beat " + opponentTeamName + splitResultTxt + "by " + Integer.valueOf(match.getMatch().getMatchResult().split(",")[1]) 
									+ " run" + Plural(Integer.valueOf(match.getMatch().getMatchResult().split(",")[1]));
							}
							break;
						default:
							resultToShow = resultToShow + " win by " + Integer.valueOf(match.getMatch().getMatchResult().split(",")[1]) 
								+ " run" + Plural(Integer.valueOf(match.getMatch().getMatchResult().split(",")[1]));
							break;
						}
					} else if (match.getMatch().getMatchResult().toUpperCase().contains(CricketUtil.WICKET)) {
						
						switch (broadcaster) {
						case "ICC-U19-2023": case "T20_MUMBAI":
							if(splitResultTxt.isEmpty()) {
								resultToShow = resultToShow + " beat " + opponentTeamName + " by " + Integer.valueOf(match.getMatch().getMatchResult().split(",")[1]) 
									+ " wicket" + Plural(Integer.valueOf(match.getMatch().getMatchResult().split(",")[1]));
							} else {
								resultToShow = resultToShow + " beat " + opponentTeamName + splitResultTxt + "by " + Integer.valueOf(match.getMatch().getMatchResult().split(",")[1]) 
									+ " wicket" + Plural(Integer.valueOf(match.getMatch().getMatchResult().split(",")[1]));
							}
							break;
						default:
							resultToShow = resultToShow + " win by " + Integer.valueOf(match.getMatch().getMatchResult().split(",")[1]) 
								+ " wicket" + Plural(Integer.valueOf(match.getMatch().getMatchResult().split(",")[1]));
							if(ballsRemaining) {
								TargetData targetData = GetTargetData(match);
								if(targetData.getRemaningBall() > 0) {
									if(targetData.getRemaningBall() > 120) {
										resultToShow = resultToShow + " with " + CricketFunctions.OverBalls(0, targetData.getRemaningBall()) 
											+ " overs remaining";
									} else {
										resultToShow = resultToShow + " with " + targetData.getRemaningBall() 
											+ " ball" + CricketFunctions.Plural(targetData.getRemaningBall()) + " remaining";
									}
								}
							}
							break;
						}
					}
					if(match.getMatch().getMatchResult().toUpperCase().contains(CricketUtil.DLS)) {
						resultToShow = resultToShow + " (" + CricketUtil.DLS + ")";
					}else if(match.getMatch().getMatchResult().toUpperCase().contains(CricketUtil.VJD)) {
						resultToShow = resultToShow + " (" + CricketUtil.VJD + ")";
					}
				}
			}
		}
		return resultToShow;
	}

	public static String OverBalls(int Overs,int Balls) {
		
		int TotalBalls=0, WholeOv, OddBalls;
		String Overs_text = "0" ;
		
		TotalBalls = 6 * Overs + Balls ;

		if(TotalBalls > 0) {
			WholeOv = ((TotalBalls)/6);
			OddBalls = (TotalBalls - 6 * (WholeOv));
			if(OddBalls == 0) {
				Overs_text = String.valueOf(WholeOv);
			} else {
				Overs_text = String.valueOf(WholeOv)+"."+String.valueOf(OddBalls);
			}
		}
		
		return Overs_text;
		
	}	
	
	public static String generateTossResult(MatchAllData match,String TossType,String DecisionType, String teamNameType, String electedOrChoose) {

		String TeamNameToUse="", decisionText = ""; 
		
		switch (match.getSetup().getTossWinningDecision()) {
		case CricketUtil.BAT:
			decisionText = CricketUtil.BAT.toLowerCase();
			break;
		default:
			switch (DecisionType) {
			case CricketUtil.FIELD:
				decisionText = CricketUtil.FIELD.toLowerCase();
				break;
			default:
				decisionText = CricketUtil.BOWL;
				break;
			}
			break;
		}
		switch (teamNameType) {
		case CricketUtil.TEAM_BADGE:
			if(match.getSetup().getTossWinningTeam() == match.getSetup().getHomeTeamId()) {
				TeamNameToUse = match.getSetup().getHomeTeam().getTeamBadge();
			} else {
				TeamNameToUse = match.getSetup().getAwayTeam().getTeamBadge();
			}
		    break;
		case CricketUtil.SHORT:
			if(match.getSetup().getTossWinningTeam() == match.getSetup().getHomeTeamId()) {
				TeamNameToUse = match.getSetup().getHomeTeam().getTeamName4();
			} else {
				TeamNameToUse = match.getSetup().getAwayTeam().getTeamName4();
			}
		    break;
		case CricketUtil.MIDDLE:
			if(match.getSetup().getTossWinningTeam() == match.getSetup().getHomeTeamId()) {
				TeamNameToUse = match.getSetup().getHomeTeam().getTeamName3();
			} else {
				TeamNameToUse = match.getSetup().getAwayTeam().getTeamName3();
			}
		    break;
		default:
			if(match.getSetup().getTossWinningTeam() == match.getSetup().getHomeTeamId()) {
				TeamNameToUse = match.getSetup().getHomeTeam().getTeamName1();
			} else {
				TeamNameToUse = match.getSetup().getAwayTeam().getTeamName1();
			}
		    break;
		}
		switch (TossType) {
		case CricketUtil.MINI:
			return CricketUtil.TOSS + ": " + TeamNameToUse;
		case CricketUtil.SHORT:
			return TeamNameToUse + " won the toss & " + decisionText;
		case "MEDIUM":
			switch (electedOrChoose) {
			case CricketUtil.ELECTED:
				return TeamNameToUse + " won | elected to " + decisionText;	
			default:
				return TeamNameToUse + " won | chose to " + decisionText;	
			}	
		default:
			if(electedOrChoose == null) {
				return TeamNameToUse + " won the toss";
			} else {
				switch (electedOrChoose) {
				case CricketUtil.ELECTED:
					return TeamNameToUse + " won the toss & elected to " + decisionText;
				default:
					return TeamNameToUse + " won the toss & chose to " + decisionText;
				}
			}
		}
	}	
	
	public static int getWicketsLeft(MatchAllData match, int whichInning) {
		
		int wicketsLeft = 0;
		
		if(match.getSetup().getMaxOvers() == 1) {
			wicketsLeft = 2 - (match.getMatch().getInning().get(whichInning-1).getTotalWickets()); 
		} else {
			wicketsLeft = 10 - (match.getMatch().getInning().get(whichInning-1).getTotalWickets()); 
		}
		
		if(wicketsLeft <= 0) {
			wicketsLeft = 0;
		}
		
		return wicketsLeft;
	}	
	
	public static TargetData GenerateMatchSummaryStatus(int whichInning, MatchAllData match, String teamNameType, 
		String SplitSummaryText, String broadcaster, boolean ballsRemaining) 
	{
		TargetData targetData = new TargetData();
		
		if(match.getMatch().getInning().get(0).getTotalRuns() > 0 || (6 * match.getMatch().getInning().get(0).getTotalOvers() 
			+ match.getMatch().getInning().get(0).getTotalBalls()) > 0) 
		{
			targetData = new TargetData(GenerateMatchResult(match, teamNameType, broadcaster, 
				SplitSummaryText, ballsRemaining));
			if(!targetData.getTargetOrResult().trim().isEmpty()) {
				
				targetData.setMatchFinished(true);
				
			} else {
				
		    	int lead_by = GetTeamRunsAhead(whichInning,match);
	    		targetData = GetTargetData(match);
				String batTeamNm = "", bowlTeamNm = "";
				switch (teamNameType) {
			    case CricketUtil.SHORT: 
			    	batTeamNm = match.getMatch().getInning().get(whichInning - 1).getBatting_team().getTeamName4();
			    	bowlTeamNm = match.getMatch().getInning().get(whichInning - 1).getBowling_team().getTeamName4();
			    	break;
			    case CricketUtil.MIDDLE: 
			    	batTeamNm = match.getMatch().getInning().get(whichInning - 1).getBatting_team().getTeamName3();
			    	bowlTeamNm = match.getMatch().getInning().get(whichInning - 1).getBowling_team().getTeamName3();
			    	break;
			    default: 
			    	batTeamNm = (match.getMatch().getInning().get(whichInning - 1)).getBatting_team().getTeamName1();
			    	bowlTeamNm = (match.getMatch().getInning().get(whichInning - 1)).getBowling_team().getTeamName1();
			    	break;
			    }
				
			    switch (whichInning) {
			    case 1:
			    	if ((match.getMatch().getInning().get(whichInning - 1).getTotalRuns() > 0) || 
			  		      (match.getMatch().getInning().get(whichInning - 1).getTotalOvers() > 0) || 
			  		      (match.getMatch().getInning().get(whichInning - 1).getTotalBalls() > 0)) {
			    		targetData.setTargetOrResult("Current Run Rate " + (match.getMatch().getInning().get(0)).getRunRate());
			  		    }
			    	else {
			    		targetData.setTargetOrResult(CricketFunctions.generateTossResult(match, CricketUtil.FULL, 
			    			CricketUtil.FIELD, CricketUtil.FULL, CricketUtil.CHOSE));
			    	}
			    	break;
			    	
			    case 2: case 3:
			    	
			    	if(match.getSetup().getMaxOvers() <= 0) { //Test & FC matches

			    		if(lead_by >= 0) {
				    		if(lead_by > 0) {
				    			targetData.setTargetOrResult(batTeamNm + " lead by " + lead_by + " run" + Plural(lead_by));
				    		} else if(lead_by == 0) {
				    			targetData.setTargetOrResult("Scores are level");
				    		}
			    		} else {
			    			if(whichInning == 3 && CricketFunctions.getWicketsLeft(match,whichInning) <= 0) {
			    				targetData.setTargetOrResult(bowlTeamNm + " win by innings & " + (-1 * lead_by) 
			    					+ " run" + Plural(-1 * lead_by));
			    				targetData.setMatchFinished(true);
			    			} else {
			    				targetData.setTargetOrResult(batTeamNm + " trail by " + (-1 * lead_by) 
			    					+ " run" + Plural(-1 * lead_by));
			    			}
			    		}
				    		
			    	} else { //Limited overs matches
			    		
					    if (targetData.getRemaningRuns() > 0 && targetData.getRemaningBall() > 0 && (CricketFunctions.getWicketsLeft(match,whichInning) > 0)) {
					    	
					    	switch (broadcaster) {
					    	case "ICC-U19-2023":
					    		if(targetData.getRemaningRuns() == 1) {
					    			targetData.setTargetOrResult(batTeamNm + " need " + targetData.getRemaningRuns() + 
								        " run" + CricketFunctions.Plural(targetData.getRemaningRuns()) + " to win from ");
						    	}else {
						    		targetData.setTargetOrResult(batTeamNm + " need " + targetData.getRemaningRuns() + 
								        " more run" + CricketFunctions.Plural(targetData.getRemaningRuns()) + " to win from ");
						    	}
					    		break;
					    	default:
					    		targetData.setTargetOrResult(batTeamNm + " need " + targetData.getRemaningRuns() + 
					        		" run" + CricketFunctions.Plural(targetData.getRemaningRuns()) + " to win from ");
					    		break;
					    	}
					    	
					    	if (targetData.getRemaningBall() > 120) {
					    		targetData.setTargetOrResult(targetData.getTargetOrResult() 
					    			+ CricketFunctions.OverBalls(0,targetData.getRemaningBall()) + " overs");
							} else {
								targetData.setTargetOrResult(targetData.getTargetOrResult() + targetData.getRemaningBall() + 
									" ball" + CricketFunctions.Plural(targetData.getRemaningBall()));
							}
					    } 
					    else if (targetData.getRemaningRuns() <= 0) 
					    {
					    	switch (broadcaster) {
							case "ICC-U19-2023": case "T20_MUMBAI":
								if(match.getSetup().getMatchType().equalsIgnoreCase(CricketUtil.SUPER_OVER)) {
									if(SplitSummaryText.isEmpty()) {
										targetData.setTargetOrResult(batTeamNm + " beat " + bowlTeamNm + " in the super over");
									} else {
										targetData.setTargetOrResult(batTeamNm + " beat " + bowlTeamNm 
											+ SplitSummaryText + "in the super over");
									}
								}else {
									if(SplitSummaryText.isEmpty()) {
										targetData.setTargetOrResult(batTeamNm + " beat " + bowlTeamNm + " by " + CricketFunctions.getWicketsLeft(match,whichInning) + 
									    		" wicket" + CricketFunctions.Plural(CricketFunctions.getWicketsLeft(match,whichInning)));
									} else {
										targetData.setTargetOrResult(batTeamNm + " beat " + bowlTeamNm + SplitSummaryText 
											+ "by " + CricketFunctions.getWicketsLeft(match,whichInning) + " wicket" 
											+ CricketFunctions.Plural(CricketFunctions.getWicketsLeft(match,whichInning)));
									}
								}
								targetData.setMatchFinished(true);
								break;
							default:
								if(match.getSetup().getMatchType().equalsIgnoreCase(CricketUtil.SUPER_OVER)) {
									targetData.setTargetOrResult(batTeamNm + " win the super over");
								} else {
									targetData.setTargetOrResult(batTeamNm + " win by " + CricketFunctions.getWicketsLeft(match,whichInning) + 
							    		" wicket" + CricketFunctions.Plural(CricketFunctions.getWicketsLeft(match,whichInning))); 
									if(ballsRemaining) {
										if(targetData.getRemaningBall() > 0) {
											if(targetData.getRemaningBall() <= 120) {
												targetData.setTargetOrResult(targetData.getTargetOrResult() + " with " + targetData.getRemaningBall() 
													+ " ball" + CricketFunctions.Plural(targetData.getRemaningBall()) + " remaining");
											}else {
												targetData.setTargetOrResult(targetData.getTargetOrResult() + " with " + 
													CricketFunctions.OverBalls(0, targetData.getRemaningBall()) + " overs remaining");
											}
										}
									}
								}
								targetData.setMatchFinished(true);
								break;
							}
					    }
					    else if (targetData.getRemaningRuns() == 1 && (targetData.getRemaningBall() <= 0 
					    	|| CricketFunctions.getWicketsLeft(match,whichInning) <= 0)) {
					    	switch (broadcaster) {
							case "ICC-U19-2023": case "T20_MUMBAI": case "NPL": case "BENGAL-T20": case "ISPL":
								if(match.getSetup().getMatchType().equalsIgnoreCase(CricketUtil.SUPER_OVER)) {
									targetData.setTargetOrResult("Super Over tied - Another super over to follow");
								}else {
									if(SplitSummaryText.isEmpty()) {
										targetData.setTargetOrResult("Match tied - super over to follow");
									} else {
										targetData.setTargetOrResult("Match tied" + SplitSummaryText + "super over to follow");
									}
								}
								break;
							default:
								if(match.getSetup().getMatchType().equalsIgnoreCase(CricketUtil.SUPER_OVER)) {
									targetData.setTargetOrResult("Super Over tied - Another super over to follow");
								}else {
									if(SplitSummaryText.isEmpty()) {
										targetData.setTargetOrResult("Match tied - winner will be decided by super over");
									} else {
										targetData.setTargetOrResult("Match tied" + SplitSummaryText + "winner will be decided by super over");
									}
								}
								break;
							}
							targetData.setMatchFinished(true);
					    } 
					    else 
					    {
					    	switch (broadcaster) {
							case "ICC-U19-2023": case "T20_MUMBAI":
								if(match.getSetup().getMatchType().equalsIgnoreCase(CricketUtil.SUPER_OVER)) {
									if(SplitSummaryText.isEmpty()) {
										targetData.setTargetOrResult(bowlTeamNm + " beat " + batTeamNm + " in the super over");
									} else {
										targetData.setTargetOrResult(bowlTeamNm + " beat " + batTeamNm + SplitSummaryText + "in the super over");
									}
								}else {
									if(SplitSummaryText.isEmpty()) {
										targetData.setTargetOrResult(bowlTeamNm + " beat " + batTeamNm + " by " + (targetData.getRemaningRuns() - 1) + 
								    		" run" + CricketFunctions.Plural(targetData.getRemaningRuns() - 1));
									} else {
										targetData.setTargetOrResult(bowlTeamNm + " beat " + batTeamNm + SplitSummaryText + "by " + (targetData.getRemaningRuns() - 1) + 
								    		" run" + CricketFunctions.Plural(targetData.getRemaningRuns() - 1));
									}
								}
								targetData.setMatchFinished(true);
								break;
							default:
								if(match.getSetup().getMatchType().equalsIgnoreCase(CricketUtil.SUPER_OVER)) {
									targetData.setTargetOrResult(bowlTeamNm + " win the super over");
								} else {
									targetData.setTargetOrResult(bowlTeamNm + " win by " + (targetData.getRemaningRuns() - 1) + 
							    		" run" + CricketFunctions.Plural(targetData.getRemaningRuns() - 1));
								}
								targetData.setMatchFinished(true);
								break;
							}
					    }
					    if(match.getSetup().getTargetType() != null) {
							if(match.getSetup().getTargetType().equalsIgnoreCase(CricketUtil.DLS)) {
								targetData.setTargetOrResult(targetData.getTargetOrResult() + " (" + CricketUtil.DLS + ")");
							}else if(match.getSetup().getTargetType().equalsIgnoreCase(CricketUtil.VJD)) {
								targetData.setTargetOrResult(targetData.getTargetOrResult() + " (" + CricketUtil.VJD + ")");
							}
					    }
			    	}
			    	break;
			    case 4:
			    	if((1 - lead_by) <= 0) {
			    		targetData.setTargetOrResult(batTeamNm + " win by " + CricketFunctions.getWicketsLeft(match,whichInning) 
			    			+ " wicket" + CricketFunctions.Plural(CricketFunctions.getWicketsLeft(match,whichInning)));
						targetData.setMatchFinished(true);
			    	} else {
			    		if(CricketFunctions.getWicketsLeft(match,whichInning) <= 0) {
			    			if(targetData.getRemaningRuns() == 1) {
			    				targetData.setTargetOrResult("match tied");
			    			} else {
			    				targetData.setTargetOrResult(bowlTeamNm + " win by " + (targetData.getRemaningRuns() - 1) + 
						    		" run" + CricketFunctions.Plural(targetData.getRemaningRuns() - 1));
			    			}
			    			targetData.setMatchFinished(true);
			    		} else {
				    		if(match.getMatch().getInning().get(whichInning - 1).getTotalRuns() == 0) {
				    			targetData.setTargetOrResult(batTeamNm + " need " + (1 - lead_by) + " run" + CricketFunctions.Plural((1 - lead_by)) + " to win");
				    		} else {
				    			targetData.setTargetOrResult(batTeamNm + " need " + (1 - lead_by) + " run" + CricketFunctions.Plural((1 - lead_by)) + " to win");
				    		}
			    		}
			    	}		    	
			    	break;
			    }
			}
		} else {
    		targetData.setTargetOrResult(CricketFunctions.generateTossResult(match, CricketUtil.FULL, 
       			CricketUtil.FIELD, CricketUtil.FULL, CricketUtil.CHOSE));
		}
	    return targetData;
	}
	
    public static String findConsecutiveDupicateEvents(List<Event> allEvents, Event currentEvent) {

        if (allEvents.size() > 0) {
            Event lastEvent = allEvents.get(allEvents.size() - 1);
            lastEvent.setEventNumber(currentEvent.getEventNumber());
            try {
                String lastJson = objectMapper.writeValueAsString(lastEvent);
                String currentJson = objectMapper.writeValueAsString(currentEvent);
                if (lastJson.equals(currentJson)) {
                    return CricketUtil.BACK_TO_BACK + " " + currentEvent.getEventType();
                }
            } catch (JsonProcessingException e) {
                throw new RuntimeException("Error converting Event to JSON", e);
            }
        }
        return "";
    }
	
	public static String generateStrikeRate(int runs, int balls, int numberOfDecimals) {
		
		String strike_rate = "";
		if (balls > 0) {
			float sr_val = (100 / (float) balls) * (float) runs;
			switch (numberOfDecimals) {
			case 0: 
				return String.valueOf((int)Math.round(sr_val));
			case 1:
				strike_rate = String.format("%.01f", sr_val);
				break;
			default:
				strike_rate = String.format("%.02f", sr_val);
				break;
			}
		}
		if(strike_rate.contains(".") && strike_rate.split("\\.")[1].charAt(0) == '0') {
			return strike_rate.split("\\.")[0];
		}
		return strike_rate;
	}
	
	public static String getOnlineCurrentDate() throws MalformedURLException, IOException
	{
		HttpURLConnection httpCon = (HttpURLConnection) new URL("https://mail.google.com/").openConnection();
		return new SimpleDateFormat("yyyy-MM-dd").format(new Date(httpCon.getDate()));
	}	

	public static String generateRunRate(int runs, int overs, int balls, int numberOfDecimals, MatchAllData match) {
		
		String run_rate = "";
		int total_balls = (overs * Integer.valueOf(match.getSetup().getBallsPerOver())) + balls;
		if (total_balls > 0) {
			float run_rate_val = ((float) runs / (float) total_balls) * Integer.valueOf(match.getSetup().getBallsPerOver());
			switch (numberOfDecimals) {
			case 1:
				run_rate = String.format("%.01f", run_rate_val);
				break;
			default:
				run_rate = String.format("%.02f", run_rate_val);
				break;
			}
		} else if (total_balls == 0) {
			switch (numberOfDecimals) {
			case 1:
				run_rate = String.format("%.01f", (float) total_balls);
				break;
			default:
				run_rate = String.format("%.02f", (float) total_balls);
				break;
			}
		} else if (balls < 0) {
			run_rate = "-";
		}
		return run_rate;
	}
	
	public static List<Player> getPlayersFromDB(CricketService cricketService, String whichTeamToProcess, MatchAllData match)
	{
		List<Player> players = new ArrayList<Player>(),whichTeamToCheck = new ArrayList<Player>();
		boolean player_found = false; 
		int whichTeamId = 0; 
		
		switch (whichTeamToProcess) {
		case CricketUtil.HOME:
			whichTeamId = match.getSetup().getHomeTeamId();
			whichTeamToCheck = match.getSetup().getHomeSquad();
			break;
		case CricketUtil.AWAY:
			whichTeamId = match.getSetup().getAwayTeamId();
			whichTeamToCheck = match.getSetup().getAwaySquad();
			break;
		}
		for(Player plyr : cricketService.getPlayers(CricketUtil.TEAM,String.valueOf(whichTeamId))) {
			player_found = false;
			for(Player subPlyr : whichTeamToCheck) {
				if (subPlyr.getPlayerId() == plyr.getPlayerId()) {
					player_found = true;
				}
			}
			if(player_found == false) {
				players.add(plyr);
			}
		}
		return players;
	}
	
    public static String RemoveUnicodeCharacters(String input) {
        if (input == null) return null;

        // Step 1: Normalize known tricky characters
        String cleaned = input
            // spaces
            .replace('\u202F', ' ')  // narrow no-break space
            .replace('\u00A0', ' ')  // non-breaking space
            .replace('\u2007', ' ')  // figure space
            .replace('\u2060', ' ')  // word joiner (invisible)
            // hyphens & dashes
            .replace('\u2011', '-')  // non-breaking hyphen
            .replace('\u2010', '-')  // hyphen
            .replace('\u2012', '-')  // figure dash
            .replace('\u2013', '-')  // en dash
            .replace('\u2014', '-')  // em dash
            .replace('\u2212', '-')  // minus sign
            .replace('\uFE58', '-')  // small em dash
            .replace('\uFE63', '-')  // small hyphen-minus
            .replace('\uFF0D', '-'); // fullwidth hyphen-minus

        // Step 2: Replace any other Unicode space separators with normal space
        cleaned = cleaned.replaceAll("\\p{Zs}+", " ");

        // Step 3: Remove other invisible control characters (if any)
        cleaned = cleaned.replaceAll("\\p{Cc}+", "");

        // Step 4: Trim leading and trailing spaces
        return cleaned.trim();
    }	
    
	public static MatchAllData populateMatchVariables(CricketService cricketService, MatchAllData match) 
			throws IllegalAccessException, InvocationTargetException 
	{
		List<Player> players = new ArrayList<Player>();
		
		for(Player plyr:match.getSetup().getHomeSquad()) {
			players.add(populatePlayer(cricketService, plyr, match));
		}
		match.getSetup().setHomeSquad(players);

		if(match.getSetup().getHomeSubstitutes() != null) {
			players = new ArrayList<Player>();
			for(Player plyr:match.getSetup().getHomeSubstitutes()) {
				players.add(populatePlayer(cricketService, plyr, match));
			}
			match.getSetup().setHomeSubstitutes(players);
			
			players = new ArrayList<Player>();
			if(match.getSetup().getHomeOtherSquad() != null) {
				for(Player plyr:match.getSetup().getHomeOtherSquad()) {
					players.add(populatePlayer(cricketService, plyr, match));
				}
			}
			match.getSetup().setHomeOtherSquad(players);
		}
		
		if(match.getSetup().getAwaySubstitutes() != null) {
			players = new ArrayList<Player>();
			for(Player plyr:match.getSetup().getAwaySubstitutes()) {
				players.add(populatePlayer(cricketService, plyr, match));
			}
			match.getSetup().setAwaySubstitutes(players);
			
			players = new ArrayList<Player>();
			if(match.getSetup().getAwayOtherSquad() != null) {
				for(Player plyr:match.getSetup().getAwayOtherSquad()) {
					players.add(populatePlayer(cricketService, plyr, match));
				}
			}
			match.getSetup().setAwayOtherSquad(players);
		}
		
		players = new ArrayList<Player>();
		for(Player plyr:match.getSetup().getAwaySquad()) {
			players.add(populatePlayer(cricketService, plyr, match));
		}
		match.getSetup().setAwaySquad(players);

		if(match.getSetup().getHomeTeamId() > 0)
			match.getSetup().setHomeTeam(cricketService.getTeam(CricketUtil.TEAM, String.valueOf(match.getSetup().getHomeTeamId())));
		if(match.getSetup().getAwayTeamId() > 0)
			match.getSetup().setAwayTeam(cricketService.getTeam(CricketUtil.TEAM, String.valueOf(match.getSetup().getAwayTeamId())));
		if(match.getSetup().getGroundId() > 0) {
			match.getSetup().setGround(cricketService.getGround(match.getSetup().getGroundId()));
			if(match.getSetup().getGround() != null) {
				match.getSetup().setVenueName(match.getSetup().getGround().getFullname());
			}
		}
		
		if(match.getMatch().getInning() != null) {
			for(Inning inn : match.getMatch().getInning()) {
				
				inn.setBatting_team(cricketService.getTeam(CricketUtil.TEAM, String.valueOf(inn.getBattingTeamId())));
				inn.setBowling_team(cricketService.getTeam(CricketUtil.TEAM, String.valueOf(inn.getBowlingTeamId())));
				
				if(inn.getBattingCard() != null)
					for(BattingCard batc:inn.getBattingCard()) 
						batc = processBattingcard(cricketService,batc);
	
				if(inn.getPartnerships() != null)
					for(Partnership part:inn.getPartnerships()) {
						part.setFirstPlayer(cricketService.getPlayer(CricketUtil.PLAYER, String.valueOf(part.getFirstBatterNo())));
						part.setSecondPlayer(cricketService.getPlayer(CricketUtil.PLAYER, String.valueOf(part.getSecondBatterNo())));
					}
				
				if(inn.getBowlingCard() != null)
					for(BowlingCard bowlc:inn.getBowlingCard())
						bowlc.setPlayer(cricketService.getPlayer(CricketUtil.PLAYER, String.valueOf(bowlc.getPlayerId())));
	
				if(inn.getBowlingTeamId() == match.getSetup().getHomeTeamId()) {
					inn.setFielders(match.getSetup().getHomeSquad());
				} else if(inn.getBowlingTeamId() == match.getSetup().getAwayTeamId()) {
					inn.setFielders(match.getSetup().getAwaySquad());
				}
	
			}
		}
		return match;
	}   

	public static MatchAllData readOrSaveMatchFile(String whatToProcess, String whichFileToProcess, MatchAllData match, boolean backUpAllFiles) 
		throws JAXBException, StreamWriteException, DatabindException, IOException, URISyntaxException
	{
		switch (whatToProcess) {
		case CricketUtil.WRITE:
			if(whichFileToProcess.toUpperCase().contains(CricketUtil.SETUP)) {
				Files.write(Paths.get(CricketUtil.CRICKET_DIRECTORY + CricketUtil.SETUP_DIRECTORY 
					+ match.getMatch().getMatchFileName()), 
					objectWriter.writeValueAsString(match.getSetup()).getBytes());			
				if(backUpAllFiles == true) {
					Files.write(Paths.get(CricketUtil.CRICKET_DIRECTORY + CricketUtil.BACK_UP_DIRECTORY 
						+ CricketUtil.SETUP_DIRECTORY + match.getMatch().getMatchFileName()), 
						objectWriter.writeValueAsString(match.getSetup()).getBytes());			
				}
			}
			if(match.getSetup().getMatchDataUpdate() != null && match.getSetup().getMatchDataUpdate().equalsIgnoreCase(CricketUtil.START)) {
				if(whichFileToProcess.toUpperCase().contains(CricketUtil.EVENT)) {
					Files.write(Paths.get(CricketUtil.CRICKET_DIRECTORY + CricketUtil.EVENT_DIRECTORY 
						+ match.getMatch().getMatchFileName()), 
						objectWriter.writeValueAsString(match.getEventFile()).getBytes());
					if(backUpAllFiles == true) {
						Files.write(Paths.get(CricketUtil.CRICKET_DIRECTORY + CricketUtil.BACK_UP_DIRECTORY 
							+ CricketUtil.EVENT_DIRECTORY + match.getMatch().getMatchFileName()),
							objectWriter.writeValueAsString(match.getEventFile()).getBytes());			
					}
				}
				if(whichFileToProcess.toUpperCase().contains(CricketUtil.MATCH)) {
					Files.write(Paths.get(CricketUtil.CRICKET_DIRECTORY + CricketUtil.MATCHES_DIRECTORY 
						+ match.getMatch().getMatchFileName()), 
						objectWriter.writeValueAsString(match.getMatch()).getBytes());
					if(backUpAllFiles == true) {
						Files.write(Paths.get(CricketUtil.CRICKET_DIRECTORY + CricketUtil.BACK_UP_DIRECTORY 
							+ CricketUtil.MATCHES_DIRECTORY + match.getMatch().getMatchFileName()), 
							objectWriter.writeValueAsString(match.getMatch()).getBytes());			
					}
				}
			}
			break;
		case CricketUtil.READ:
			if(whichFileToProcess.toUpperCase().contains(CricketUtil.SETUP)) {
				if(new File(CricketUtil.CRICKET_DIRECTORY + CricketUtil.SETUP_DIRECTORY + match.getMatch().getMatchFileName()).exists() == true) {
					match.setSetup(new ObjectMapper().readValue(new File(CricketUtil.CRICKET_DIRECTORY 
						+ CricketUtil.SETUP_DIRECTORY + match.getMatch().getMatchFileName()), Setup.class));
				}
			}
			if(whichFileToProcess.toUpperCase().contains(CricketUtil.EVENT)) {
				if(new File(CricketUtil.CRICKET_DIRECTORY 
					+ CricketUtil.EVENT_DIRECTORY + match.getMatch().getMatchFileName()).exists() == true) {
					match.setEventFile(new ObjectMapper().readValue(new File(CricketUtil.CRICKET_DIRECTORY 
						+ CricketUtil.EVENT_DIRECTORY + match.getMatch().getMatchFileName()), EventFile.class));
					}
			}
			if(whichFileToProcess.toUpperCase().contains(CricketUtil.MATCH)) {
				match.setMatch(new ObjectMapper().readValue(new File(CricketUtil.CRICKET_DIRECTORY 
					+ CricketUtil.MATCHES_DIRECTORY + match.getMatch().getMatchFileName()), Match.class));
			}
			break;
		}
		return match;
	}
	
	public static BattingCard processBattingcard(CricketService cricketService,BattingCard bc)
	{
		bc.setPlayer(cricketService.getPlayer(CricketUtil.PLAYER, String.valueOf(bc.getPlayerId())));
		if (bc.getConcussionPlayerId() > 0) {
			bc.setConcussion_player(cricketService.getPlayer(CricketUtil.PLAYER, 
				String.valueOf(bc.getConcussionPlayerId())));
		}
		
		if(bc.getStatus() != null && bc.getStatus().equalsIgnoreCase(CricketUtil.OUT)) {

			switch (bc.getHowOut().toUpperCase()) {
			case CricketUtil.CAUGHT_AND_BOWLED: case CricketUtil.CAUGHT: case CricketUtil.BOWLED: 
			case CricketUtil.STUMPED: case CricketUtil.LBW: case CricketUtil.HIT_WICKET: case CricketUtil.MANKAD:
				bc.setHowOutBowler(cricketService.getPlayer(CricketUtil.PLAYER, String.valueOf(bc.getHowOutBowlerId())));
				break;
			}
			
			switch (bc.getHowOut().toUpperCase()) {
			case CricketUtil.CAUGHT: case CricketUtil.STUMPED: case CricketUtil.RUN_OUT:  
				bc.setHowOutFielder(cricketService.getPlayer(CricketUtil.PLAYER, String.valueOf(bc.getHowOutFielderId())));
				break;
			}
			switch (bc.getHowOut().toUpperCase()) {
			case CricketUtil.CAUGHT_AND_BOWLED:
				bc.setHowOutText("c & b " + bc.getHowOutBowler().getTicker_name());
				bc.setHowOutPartOne("c & b");
				bc.setHowOutPartTwo(bc.getHowOutBowler().getTicker_name());
				break;
			case CricketUtil.CAUGHT: case CricketUtil.MANKAD: case CricketUtil.RUN_OUT:
				switch (bc.getHowOut().toUpperCase()) {
				case CricketUtil.CAUGHT:
					if(bc.getHowOutFielderId() < 0) {
						bc.setHowOutText("c substitute");
						bc.setHowOutPartOne("c substitute");
					} else {
						bc.setHowOutText("c " + bc.getHowOutFielder().getTicker_name());
						bc.setHowOutPartOne("c " + bc.getHowOutFielder().getTicker_name());
						if(bc.getWasHowOutFielderSubstitute() != null && bc.getWasHowOutFielderSubstitute().equalsIgnoreCase(CricketUtil.YES)) {
							bc.setHowOutText(bc.getHowOutText() + " (SUB)");
							bc.setHowOutPartOne(bc.getHowOutPartOne() + " (SUB)");
						}
					}
					bc.setHowOutText(bc.getHowOutText() + " b " + bc.getHowOutBowler().getTicker_name());
					bc.setHowOutPartTwo("b " + bc.getHowOutBowler().getTicker_name());
					break;
				case CricketUtil.RUN_OUT:
					bc.setHowOutPartOne("run out");
					if(bc.getHowOutFielderId() < 0) {
						bc.setHowOutText("run out substitute");
						bc.setHowOutPartTwo("substitute");
					} else {
						bc.setHowOutText("run out (" + bc.getHowOutFielder().getTicker_name() + ")");
						bc.setHowOutPartTwo(bc.getHowOutFielder().getTicker_name());
						if(bc.getWasHowOutFielderSubstitute() != null && bc.getWasHowOutFielderSubstitute().equalsIgnoreCase(CricketUtil.YES)) {
							bc.setHowOutText(bc.getHowOutText() + " (SUB)");
							bc.setHowOutPartTwo(bc.getHowOutPartTwo() + " (SUB)");
						}
					}
					break;
				case CricketUtil.MANKAD:
					bc.setHowOutText("run out (" + bc.getHowOutBowler().getTicker_name() + ")");
					bc.setHowOutPartOne("run out");
					bc.setHowOutPartTwo(bc.getHowOutBowler().getTicker_name());
					break;
				}
				break;
			case CricketUtil.BOWLED:
				bc.setHowOutText("b " + bc.getHowOutBowler().getTicker_name());
				bc.setHowOutPartOne("");
				bc.setHowOutPartTwo("b " + bc.getHowOutBowler().getTicker_name());
				break;
			case CricketUtil.STUMPED:
				if(bc.getHowOutFielderId() < 0) {
					bc.setHowOutText("st substitute");
					bc.setHowOutPartOne("st substitute");
				} else {
					bc.setHowOutText("st " + bc.getHowOutFielder().getTicker_name());
					bc.setHowOutPartOne("st " + bc.getHowOutFielder().getTicker_name());
					if(bc.getWasHowOutFielderSubstitute() != null && bc.getWasHowOutFielderSubstitute().equalsIgnoreCase(CricketUtil.YES)) {
						bc.setHowOutText(bc.getHowOutText() + " (SUB)");
						bc.setHowOutPartOne(bc.getHowOutPartOne() + " (SUB)");
					}
				}
				bc.setHowOutText(bc.getHowOutText() + " b " + bc.getHowOutBowler().getTicker_name());
				bc.setHowOutPartTwo("b " + bc.getHowOutBowler().getTicker_name());
				break;
			case CricketUtil.LBW:
				System.out.println(bc.getPlayerId());
				bc.setHowOutText("lbw b " + bc.getHowOutBowler().getTicker_name());
				bc.setHowOutPartOne("lbw");
				bc.setHowOutPartTwo("b " + bc.getHowOutBowler().getTicker_name());
				break;
			case CricketUtil.HIT_WICKET:
				bc.setHowOutText("hit wicket b " + bc.getHowOutBowler().getTicker_name());
				bc.setHowOutPartOne("hit wicket");
				bc.setHowOutPartTwo("b " + bc.getHowOutBowler().getTicker_name());
				break;
			case CricketUtil.HANDLED_THE_BALL:
				bc.setHowOutText("handled the ball");
				bc.setHowOutPartOne(bc.getHowOutText());
				bc.setHowOutPartTwo("");
				break;
			case CricketUtil.HIT_BALL_TWICE:
				bc.setHowOutText("hit the ball twice");
				bc.setHowOutPartOne(bc.getHowOutText());
				bc.setHowOutPartTwo("");
				break;
			case CricketUtil.OBSTRUCTING_FIELDER:
				bc.setHowOutText("obstructing the field");
				bc.setHowOutPartOne(bc.getHowOutText());
				bc.setHowOutPartTwo("");
				break;
			case CricketUtil.TIMED_OUT:
				bc.setHowOutText("timed out");
				bc.setHowOutPartOne(bc.getHowOutText());
				bc.setHowOutPartTwo("");
				break;
			case CricketUtil.RETIRED_HURT:
				bc.setHowOutText("retired hurt");
				bc.setHowOutPartOne(bc.getHowOutText());
				bc.setHowOutPartTwo("");
				break;
			case CricketUtil.RETIRED_OUT:
				bc.setHowOutText("retired out");
				bc.setHowOutPartOne(bc.getHowOutText());
				bc.setHowOutPartTwo("");
				break;
			case CricketUtil.ABSENT_HURT:
				bc.setHowOutText("absent hurt");
				bc.setHowOutPartOne(bc.getHowOutText());
				bc.setHowOutPartTwo("");
				break;
			case CricketUtil.CONCUSSED:
				bc.setHowOutText("concussed");
				bc.setHowOutPartOne(bc.getHowOutText());
				bc.setHowOutPartTwo("");
				break;
			}
			
		}
		return bc;
	}
	
	public static Player populatePlayer(CricketService cricketService, Player player, MatchAllData match)
	{
		Player this_plyr = new Player();
		this_plyr = cricketService.getPlayer(CricketUtil.PLAYER, String.valueOf(player.getPlayerId()));
		if(this_plyr != null) {
			this_plyr.setPlayerPosition(player.getPlayerPosition()); 
			this_plyr.setCaptainWicketKeeper(player.getCaptainWicketKeeper());
		}
		return this_plyr;
	}	
}
