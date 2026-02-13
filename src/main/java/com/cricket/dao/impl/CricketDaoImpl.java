package com.cricket.dao.impl;

import java.util.List;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import com.cricket.dao.CricketDao;
import com.cricket.model.Bugs;
import com.cricket.model.Commentator;
import com.cricket.model.Dictionary;
import com.cricket.model.EverestBugs;
import com.cricket.model.FantasyImages;
import com.cricket.model.Fixture;
import com.cricket.model.Ground;
import com.cricket.model.InfobarStats;
import com.cricket.model.LeaderBoard;
import com.cricket.model.NameSuper;
import com.cricket.model.POTT;
import com.cricket.model.PerformanceBug;
import com.cricket.model.Player;
import com.cricket.model.Playoff;
import com.cricket.model.Pointers;
import com.cricket.model.Performer;
import com.cricket.model.Season;
import com.cricket.model.Split;
import com.cricket.model.Sponsor;
import com.cricket.model.Staff;
import com.cricket.model.Statistics;
import com.cricket.model.StatsType;
import com.cricket.model.Team;
import com.cricket.model.VariousText;
import com.cricket.model.Venue;
import com.cricket.model.Weather;
import com.cricket.util.CricketUtil;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Transactional
@Repository("cricketDao")
public class CricketDaoImpl implements CricketDao {

@PersistenceContext
private EntityManager entityManager;
 
@Override
public Player getPlayer(String whatToProcess, String valueToProcess) {
	switch (whatToProcess) {
	case CricketUtil.PLAYER:
        return entityManager.createQuery("SELECT p FROM Player p WHERE p.playerId = :id",Player.class)
            .setParameter("id", Long.parseLong(valueToProcess))
            .getSingleResult();		
	default:
		return null;  
	}
}

@Override
public Team getTeam(String whatToProcess, String valueToProcess) {
	switch (whatToProcess) {
	case CricketUtil.TEAM:
        return entityManager.createQuery(
            "SELECT t FROM Team t WHERE t.teamId = :id",
            Team.class)
            .setParameter("id", Long.parseLong(valueToProcess))
            .getSingleResult();
	default:
		return null;  
	}
}

@Override
public List<Team> getTeams() {
    return entityManager.createQuery("SELECT t FROM Team t", Team.class).getResultList();
}

@Override
public List<Player> getPlayers(String whatToProcess, String valueToProcess) {
	switch (whatToProcess) {
	case CricketUtil.TEAM:
        return entityManager.createQuery(
            "SELECT p FROM Player p WHERE p.teamId = :teamId",
            Player.class)
            .setParameter("teamId", Long.parseLong(valueToProcess))
            .getResultList(); 
	case CricketUtil.GENDER_SPECIFIC_TEAM:
		if(valueToProcess.contains(",")) {
	        String[] parts = valueToProcess.split(",");
	        return entityManager.createQuery(
	                "SELECT p FROM Player p WHERE p.teamId = :teamId AND p.gender = :gender",
	                Player.class)
	                .setParameter("teamId", Long.parseLong(parts[0]))
	                .setParameter("gender", parts[1])
	                .getResultList();
		} else {
			return null;  
		}
	default:
		return null;  
	}
}

@Override
public List<Ground> getGrounds() {
	return entityManager.createQuery("SELECT g FROM Ground g", Ground.class).getResultList();
}

@Override
public Ground getGround(int ground_id) {
	return entityManager.createQuery("SELECT g FROM Ground g", Ground.class).getSingleResult();
}

@Override
public StatsType getStatsType(int stats_type_id) {
	return entityManager.createQuery("SELECT s FROM StatsType g", StatsType.class).getSingleResult();
}

@Override
public List<NameSuper> getNameSupers() {
	return entityManager.createQuery("SELECT n FROM NameSuper n", NameSuper.class).getResultList();
}

@Override
public List<InfobarStats> getInfobarStats() {
	return entityManager.createQuery("SELECT n FROM InfobarStats n", InfobarStats.class).getResultList();
}

@Override
public List<Sponsor> getSponsor() {
	return entityManager.createQuery("SELECT s FROM Sponsor s", Sponsor.class).getResultList();
}

@Override
public List<FantasyImages> getFantasyImages() {
	return entityManager.createQuery("SELECT f FROM FantasyImages f", FantasyImages.class).getResultList();
}

@Override
public List<Bugs> getBugs() {
	return entityManager.createQuery("SELECT b FROM Bugs b", Bugs.class).getResultList();
}
@Override
public List<EverestBugs> getEverestBugs() {
	return entityManager.createQuery("SELECT b FROM EverestBugs b", EverestBugs.class).getResultList();
}
@Override
public List<LeaderBoard> getLeaderBoards() {
	return entityManager.createQuery("SELECT b FROM LeaderBoard b", LeaderBoard.class).getResultList();
}
@Override
public List<Split> getSplit() {
	return entityManager.createQuery("SELECT b FROM Split b", Split.class).getResultList();
}

@Override
public List<Statistics> getAllStats() {
	return entityManager.createQuery("SELECT b FROM Statistics b", Statistics.class).getResultList();
}

@Override
public List<Player> getAllPlayer() {
	return entityManager.createQuery("SELECT p FROM Player p", Player.class).getResultList();
}

@Override
public List<Fixture> getFixtures() {
	return entityManager.createQuery("SELECT p FROM Fixture p", Fixture.class).getResultList();
}

@Override
public List<VariousText> getVariousTexts() {
	return entityManager.createQuery("SELECT p FROM VariousText p", VariousText.class).getResultList();
}

@Override
public List<Venue> getVenues() {
	return entityManager.createQuery("SELECT p FROM Venue p", Venue.class).getResultList();
}

@Override
public List<Dictionary> getDictionary() {
	return entityManager.createQuery("SELECT p FROM Dictionary p", Dictionary.class).getResultList();
}
@Override
public List<Playoff> getPlayOff() {
	return entityManager.createQuery("SELECT p FROM Playoff p", Playoff.class).getResultList();
}

@Override
public List<Season> getSeasons() {
	return entityManager.createQuery("SELECT p FROM Season p", Season.class).getResultList();
}

@Override
public List<Pointers> getPointers() {
	return entityManager.createQuery("SELECT p FROM Pointers p", Pointers.class).getResultList();
}

@Override
public List<StatsType> getAllStatsType() {
	return entityManager.createQuery("SELECT p FROM StatsType p", StatsType.class).getResultList();
}

@Override
public List<Commentator> getCommentator() {
	return entityManager.createQuery("SELECT p FROM Commentator p", Commentator.class).getResultList();
}

public List<Staff> getStaff() {
	return entityManager.createQuery("SELECT p FROM Staff p", Staff.class).getResultList();
}

@Override
public List<POTT> getPott() {
	return entityManager.createQuery("SELECT p FROM POTT p", POTT.class).getResultList();
}

@Override
public List<Performer> getPerformer() {
	return entityManager.createQuery("SELECT p FROM Performer p", Performer.class).getResultList();
}

@Override
public List<Weather> getWeather() {
	return entityManager.createQuery("SELECT p FROM Weather p", Weather.class).getResultList();
}

@Override
public List<PerformanceBug> getPerformanceBugs() {
	return entityManager.createQuery("SELECT p FROM PerformanceBug p", PerformanceBug.class).getResultList();
}

@Override
public List<Player> getArchivePlayers() {
	return entityManager.createQuery("SELECT p FROM Player p", Player.class).getResultList();
}
}