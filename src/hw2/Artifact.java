package hw2;

import java.io.Serializable;

public class Artifact implements Serializable, Cloneable {
	public static String[] Genres = {"PAINTING", "SCULPTURE", "POEM"};
	private String creator;
	private String genre;
	private long ID;
	private int yearOfCreation;
	private String placeOfCreation;
	private int name;
	private int value;
	private boolean isCopy;
	
	private void generateRandomArtifact(){
		this.creator ="Artist#"+randomNum();
		this.genre =Genres[((int)(Math.random()*(Genres.length-1)))];
		this.ID =(long) (Math.random() * Long.MAX_VALUE);
		this.yearOfCreation = (int) (Math.random() * 500 + 1500);
		this.placeOfCreation = "placeOfCreation#"+randomNum();
		this.name = (int) (Math.random()*100);
		this.value = (int) (Math.random()*90000+10000);
		this.isCopy=false;
		
	}
	public Artifact(){
		generateRandomArtifact();
	}
	
	@Override
	protected Object clone() throws CloneNotSupportedException {
		// TODO Auto-generated method stub
		return super.clone();
	}
	//Return a copy of artifact with new ID
	public Artifact genCopy(){
		Artifact artifact;
		try {
			artifact = (Artifact) this.clone();
			artifact.setID((long)(Math.random() * Long.MAX_VALUE));
			artifact.setIsCopy(true);
			return artifact;
		} catch (CloneNotSupportedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
	
	public int getName() {
		return name;
	}
	public void setName(int name) {
		this.name = name;
	}
	public int getValue() {
		return value;
	}
	public void setPrice(int value) {
		this.value = value;
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
	public boolean getIsCopy() {
		return this.isCopy;
	}
	public void setIsCopy(boolean b) {
		this.isCopy = b;
	}
}
