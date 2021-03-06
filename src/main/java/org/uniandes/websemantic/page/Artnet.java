package org.uniandes.websemantic.page;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.uniandes.websemantic.file.ArtistFile;
import org.uniandes.websemantic.hibernate.HibernateSession;
import org.uniandes.websemantic.object.Artist;
import org.uniandes.websemantic.object.Artwork;

public class Artnet {

	private static String pagina = "http://www.artnet.com";
	//Paginas ya vistitadas
	private static Set<String> paginas;
	
	public static void crawling(){
		Set<Artist> artistList = new HashSet<Artist>();

		paginas = paginasYavisitadas();

		Document doc;
		try {
			String url="/artists/";
			// need http protocol
			doc = Jsoup.connect(pagina+url).get();
			paginas.add(url);
			// get all links
			Elements links = doc.select("a[href^="+url+"]");
			for (Element link : links) {
				url = link.attr("href");
				String titulo = link.text();
				if(!paginas.contains(url)){
					artistList = subPage(url,titulo);
					new ArtistFile("artnet"+titulo,artistList);
				}
			}
			HibernateSession.getInstance().closeSession();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}


	private static Set<String> paginasYavisitadas() {
		paginas = new HashSet<String>();
		List<?> listArtist = HibernateSession.getInstance().createCriteria(Artist.class);			
		for (Object artist : listArtist) {
			paginas.add(((Artist) artist).getUrl().replace("http://www.artnet.com",""));
		}
		return paginas;
	}

	private static Set<Artist> subPage(String url, String nombre) throws IOException {
		String uri = pagina+fixUrl(url);
		Document doc = Jsoup.connect(uri).get(); 
		Set<Artist> artistList = new HashSet<Artist>();
		paginas.add(url);
		String titulo = doc.title();
		if(titulo.startsWith("Browse Artists Starting with ")){
			subPageAlpha(doc);
		}
		else if(titulo.startsWith("Top 300 Artists on artnet - Most Popular Artists"))
			subPageAlpha(doc);
		else if(titulo.equals("Browse Artists on artnet - Modern and Contemporary Artists")){
			subPageAlpha(doc);
		}else{
			Artist artist = pageArtist(doc,nombre,uri);
			HibernateSession.getInstance().save(artist);
			pageArtworks(doc,artist);

		}
		return artistList;
	}

	private static void pageArtworks(Document doc, Artist artist) {
		Elements artworks = doc.select("div.col-sm-4.col-xs-6.artwork");
		for (Element arts : artworks) {
			Elements art = arts.select("p");
			Artwork artwork = new Artwork();
			artwork.setArtist(artist);
			String nameArt = art.get(1).text();
			artwork.setNombre(nameArt);
			artwork.setPrecio(art.get(3).text());
			artwork.setMuseo(art.get(2).text());
			HibernateSession.getInstance().save(artwork);
			
		}
	}


	/**
	 * corrige errores de tildes
	 * @param url
	 * @return
	 */
	private static String fixUrl(String url) {
		//		if(url.contains("uecker")) //�
		//			System.err.println("reivsar");
		if(url.contains("%c3%a0"))
			url = url.replace("%c3%a0","�");

		if(url.contains("%c3%a3"))
			url = url.replace("%c3%a3","�");


		if(url.contains("%c3%a4"))
			url = url.replace("%c3%a4","�");
		if(url.contains("%c3%af"))
			url = url.replace("%c3%af","�");
		if(url.contains("%c3%b6"))
			url = url.replace("%c3%b6","�");
		if(url.contains("%c3%bc"))
			url = url.replace("%c3%bc","�");

		if(url.contains("%c3%a9"))
			url = url.replace("%c3%a9","�");
		if(url.contains("%c3%ad"))
			url = url.replace("%c3%ad","�");
		if(url.contains("%c3%b3"))
			url = url.replace("%c3%b3","�");
		return url;
	}


	private static Artist pageArtist(Document doc,String titulo,String uri) throws IOException {
		Artist artista = new Artist();
		String name = getArtistName(doc);
		artista.setName(name);
		artista.setNacionalidad(getNationality(doc));
		artista.setAnioNacimiento(getAnioNacimiento(doc));
		artista.setAnioMuerte(getAnioMuerte(doc)); 
		artista.setUrl(uri); 
		System.out.println(artista);
		
		return artista;
	}

	/**
	 * Obtiene el a�o de muerte del artista
	 * @param doc
	 * @return
	 */
	private static String getAnioMuerte(Document doc) {
		Elements artist = doc.select("h1.text-center > span");
		artist = artist.select("span.detail > span > time");
		String anioMuerte = doc.select("span[itemprop=deathDate]").text();
		if(anioMuerte.isEmpty()){
			String heading = doc.select("div.headline > h2.detail").text();
			if(heading.contains(","))
				anioMuerte =heading.split(",")[1].replace(")", "").trim();
			if(anioMuerte.contains("�")){
				anioMuerte = anioMuerte.split("�")[1].trim();
			}else{
				anioMuerte="";
			}

		}
		//		if(anioMuerte.isEmpty()){
		//			System.err.println("no muerto?");
		//		}
		return anioMuerte;	}

	/**
	 * Obtiene el a�o de nacimiento del artista
	 * @param doc
	 * @return
	 */
	private static String getAnioNacimiento(Document doc) {
		Elements artist = doc.select("h1.text-center > span");
		artist = artist.select("span.detail > span");
		Elements time = artist.select("> time");
		String anioNacimiento;
		anioNacimiento = time.select("time[itemprop=birthDate]").text();
		if(anioNacimiento.isEmpty()){
			String heading = doc.select("div.headline > h2.detail").text();
			if(heading.contains(","))
				anioNacimiento =heading.split(",")[1].replace(")", "");
			if(anioNacimiento.contains(" born "))
				anioNacimiento = anioNacimiento.replace(" born ","");
			else if(anioNacimiento.contains("�")){
				anioNacimiento = anioNacimiento.split("�")[0].trim();
			}
		}
		if(anioNacimiento.isEmpty()){
			System.err.println("error");
		}
		return anioNacimiento;
	}


	private static String getNationality(Document doc) {
		String nationality = doc.select("span[itemprop=nationality]").text();
		if(nationality.isEmpty()){
			Elements heading = doc.select("div.headline > h2.detail");
			nationality =heading.text().split(",")[0].replace("(", "");			
		}
		if(nationality.isEmpty()){
			System.err.println("error");
		}
		return nationality;
	}

	/**
	 * Obtiene el nombre del artista
	 * @param doc
	 * @return
	 */
	private static String getArtistName(Document doc) {
		String name = doc.select("span[itemprop=name]").text();
		if(name.isEmpty()){
			Elements heading = doc.select("div.headline > h1.title");
			name =heading.text();			
		}
		if(name.isEmpty())
			System.err.println("error");
		return name;
	}

	private static void subPageAlpha(Document doc) throws IOException {
	//	Set<Artist> artistList = new HashSet<Artist>();
		Elements links = doc.select("a[href^=/artists/]");
		for (Element link : links) {
			String url = link.attr("href");
			if(!paginas.contains(url)){
				subPage(url,link.text());
				paginas.add(url);
			}
		}
	}



}