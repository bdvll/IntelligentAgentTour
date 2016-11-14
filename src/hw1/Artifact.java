package hw1;

public class Artifact {
	public String[] Genres = {"PAINTING", "SCULPTURE", "POEM"};
	private String creator;
	private String genre;
	private String ID;
	private int yearOfCreation;
	private String placeOfCreation;
	
	public Artifact(){
		generateRandomArtifact();
	}
	private void generateRandomArtifact(){
		this.creator ="Artist#"+randomNum();
		this.genre =Genres[((int) Math.random()*Genres.length) -1];
		this.ID ="ID#"+randomNum();
		this.yearOfCreation = ((int) randomNum()) % 2016;
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
	public String getID() {
		return ID;
	}
	public void setID(String iD) {
		ID = iD;
	}
}
