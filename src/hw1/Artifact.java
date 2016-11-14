package hw1;

import java.io.Serializable;

public class Artifact implements Serializable {
	public static String[] Genres = {"PAINTING", "SCULPTURE", "POEM"};
	private String creator;
	private String genre;
	private long ID;
	private int yearOfCreation;
	private String placeOfCreation;
	
	public Artifact(){
		generateRandomArtifact();
	}
	private void generateRandomArtifact(){
		this.creator ="Artist#"+randomNum();
		this.genre =Genres[((int)(Math.random()*(Genres.length-1)))];
		this.ID =(long) (Math.random() * Long.MAX_VALUE);
		this.yearOfCreation = (int) (Math.random() * 500 + 1500);
		this.placeOfCreation = "placeOfCreation#"+randomNum();
		
	}
	public String getGenre() {
		return genre;
	}
	public void setGenre(String genre) {
		this.genre = genre;
	}
	public int getYearOfCreation() {
		return yearOfCreation;
	}
	public void setYearOfCreation(int yearOfCreation) {
		this.yearOfCreation = yearOfCreation;
	}
	public String getPlaceOfCreation() {
		return placeOfCreation;
	}
	public void setPlaceOfCreation(String placeOfCreation) {
		this.placeOfCreation = placeOfCreation;
	}
	private float randomNum(){
		float hash = System.currentTimeMillis();
		return hash;
	}
	public String getCreator() {
		return creator;
	}
	public void setCreator(String creator) {
		this.creator = creator;
	}
	public long getID() {
		return ID;
	}
	public void setID(long iD) {
		ID = iD;
	}
}
