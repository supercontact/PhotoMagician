package model;
import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;

import util.SerializationControl;

// This class represents an album containing all the imported photos.
// It is saved and loaded each time the application launches.

public class AnnotatedAlbum implements Serializable {

	private static final long serialVersionUID = 1L;
	
	public ArrayList<AnnotatedPhoto> photoList;
	public ArrayList<String> tags;

	public AnnotatedAlbum() {
		photoList = new ArrayList<>();
		tags = new ArrayList<>();
	}
	
	public AnnotatedPhoto importNewPhoto(File file) {
		AnnotatedPhoto newPhoto = new AnnotatedPhoto(file);
		newPhoto.setIndex(photoList.size());
		photoList.add(newPhoto);
		return newPhoto;
	}
	
	public void removePhoto(int index) {
		photoList.get(index).setIndex(-1);
		photoList.remove(index);
		reindex();
	}
	
	public void loadAllPhotos() {
		for (AnnotatedPhoto photo : photoList) {
			photo.loadPhoto();
		}
	}
	
	public void generateAllThumbnails(int size) {
		for (AnnotatedPhoto photo : photoList) {
			photo.generateThumbnail(size);
		}
	}
	
	public boolean saveAlbum(File url) {
		return SerializationControl.save(this, url);
	}

	public static AnnotatedAlbum loadAlbum(File url) {
		if (url.exists()) {
			AnnotatedAlbum album = (AnnotatedAlbum)SerializationControl.load(url);
			album.reindex();
			album.reconstruct();
			return album;
		}
		return null;
	}
	
	private void reindex() {
		for (int i = 0; i < photoList.size(); i++) {
			photoList.get(i).setIndex(i);
		}
	}
	private void reconstruct() {
		for (int i = 0; i < photoList.size(); i++) {
			photoList.get(i).annotation.reconstruct();
		}
	}
	
}
