package com.trivago.mp.casestudy;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

/**
 * TODO: Implement this class. 
 * Your task will be to implement two functions, one for loading the data which is stored as .csv files in the ./data
 * folder and one for performing the actual search.
 */
public class HotelSearchEngineImpl implements HotelSearchEngine {

   // TODO: documentation and code cleaning...

   // TODO: Better List / Map / Object structure for easier handling / searching
   List<Hotels> hotelList = null;
   List<HotelAdvertiser> hotelAdvertiserList = null;
   List<Advertiser> advertisersList = null;
   List<Cities> citiesList = null;

   @Override
   public void initialize() {
      try {
         // Load data by class identifier
         hotelList = (List<Hotels>) loadData(Hotels.class);
         advertisersList = (List<Advertiser>) loadData(Advertiser.class);
         hotelAdvertiserList = (List<HotelAdvertiser>) loadData(HotelAdvertiser.class);
         citiesList = (List<Cities>) loadData(Cities.class);
      }
      catch (IOException e) {
         e.printStackTrace();
      }
   }

   @Override
   public List<HotelWithOffers> performSearch(String cityName, DateRange dateRange, OfferProvider offerProvider) {
      // Disclaimer: Not optimized yet. And not really pretty. Sorry for that ;-)

      // TODO: optimize data structure of loaded data!
      // First get the searched city by cityname.
      Cities city = citiesList.stream().filter(c -> c.getCityname().equals(cityName)).findFirst().get();

      // Create a Map with hotelid as key and Hotel object as value
      // this will help us to identify the offers
      Map<Integer, Hotel> cityHotelMap = hotelList.stream().filter(h -> h.getCityId() == city.getId())
            .collect(Collectors.toMap(hotel -> hotel.getId(), hotel -> hotel));

      // get advertisers from cityHotelMap... contains only hotels in the searched city
      Map<Integer, List<Integer>> advertiserHotelList = hotelAdvertiserList.stream().filter(l -> cityHotelMap.containsKey(l.getHotelId()))
            .collect(Collectors.groupingBy(HotelAdvertiser::getAdvertiserId, Collectors.mapping(HotelAdvertiser::getHotelId, Collectors.toList())));

      List<HotelWithOffers> hotelWithOffersList = new ArrayList<>();

      // Create a helper result list that contains the result of getOffersFromAdvertiser method.
      // Get the results for every advertiser
      List<Map<Integer, Offer>> resultList = getOffersFromAdvertiserList(advertiserHotelList, dateRange, offerProvider);
      /**
       * TODO 1:
       * Maybe there is a way to check whether an hotel is available at the given date range before calling 'OfferProvider.getOffersFromAdvertiser'?
       * So some advertisers would be sorted out and the costly calling would be obsolete for this advertiser.
       *
       * TODO 2:
       * Hotel caching / Advertiser caching for a specific period of time, e.g. 1 hour, 12 hours, 24 hours.
       * Depending on how often the prices are changed.
       *
       */


      // For sure there are much prettier solutions (laziness has won again here... :-( )
      // Iterate through the helper result list
      for (Map<Integer, Offer> offerMap : resultList) {
         // Iterate through the Map to create HotelWithOffer object and append it to the return result list
         Iterator<Integer> iterator = offerMap.keySet().iterator();
         while (iterator.hasNext()) {
            Integer hotelId = iterator.next();
            HotelWithOffers hotelWithOffers = null;
            try {
               hotelWithOffers = hotelWithOffersList.stream().filter(h -> h.getHotel().getId() == hotelId).findFirst().get();
               System.out.println("Katsching...");
            }
            catch (NullPointerException | NoSuchElementException e) {
               // argh, no HotelWithOffers object found...
               System.out.println("blurb :-(");
            }
            // Check if we have found an object. If not, create one and append it to the list.
            if (hotelWithOffers == null) {
               hotelWithOffers = createHotelWithOffers(cityHotelMap.get(hotelId));
               hotelWithOffersList.add(hotelWithOffers);
            }
            hotelWithOffers.getOffers().add(offerMap.get(hotelId));
         }
      }

      return hotelWithOffersList;
   }

   /**
    * Create a new instance of HotelWithOffers
    *
    * @param hotel Hotel object to set
    * @return Returns a new instance of HotelWithOffers with an instantiated Offer list.
    */
   private HotelWithOffers createHotelWithOffers(Hotel hotel) {
      HotelWithOffers hotelWithOffers = new HotelWithOffers(hotel);
      hotelWithOffers.setOffers(new ArrayList<Offer>());
      return hotelWithOffers;
   }

   /**
    * Get the offers for a specific advertiser in the searched city
    *
    * @param advertiserHotelList Contains the hotels of the advertiser
    * @param dateRange           Contains the date range
    * @param offerProvider       offerProvider object to get the prices
    * @return Returns a list of getOffersFromAdvertiser(advertiser, searchedHotelList, dateRange) results.
    */
   private List<Map<Integer, Offer>> getOffersFromAdvertiserList(Map<Integer, List<Integer>> advertiserHotelList, DateRange dateRange, OfferProvider offerProvider) {
      List<Map<Integer, Offer>> resultList = new ArrayList<>();
      Iterator<Integer> iterator = advertiserHotelList.keySet().iterator();
      while (iterator.hasNext()) {
         Integer advertiserId = iterator.next();
         // get the result for every provider
         // TODO: as mentioned above in "TODO 2", get cached values if present
         resultList.add(offerProvider.getOffersFromAdvertiser(advertisersList.get(advertiserId), advertiserHotelList.get(advertiserId), dateRange));
      }
      return resultList;
   }

   /**
    * @param clazz Class identifier to load data from csv files
    * @return Returns a list of class identifier
    * @throws IOException
    */
   public List<?> loadData(Class<?> clazz) throws IOException {
      BufferedReader br = null;
      try {
         File inputF = null;
         if (clazz.getName().equals(Cities.class.getName())) {
            inputF = new File("data/cities.csv");
         } else if (clazz.getName().equals(Hotels.class.getName())) {
            inputF = new File("data/hotels.csv");
         } else if (clazz.getName().equals(Advertiser.class.getName())) {
            inputF = new File("data/advertisers.csv");
         } else if (clazz.getName().equals(HotelAdvertiser.class.getName())) {
            inputF = new File("data/hotel_advertiser.csv");
         }

         // Fast implementation to get the contents of the csv files
         // Database would be easier ;-)
         InputStream inputFS = new FileInputStream(inputF);
         br = new BufferedReader(new InputStreamReader(inputFS));
         // skip the header of the csv
         return br.lines().skip(1).map(line -> {
            String[] p = line.split(",");
            if (clazz.getName().equals(Cities.class.getName())) {
               // get city id and city name
               if (p[1] != null && p[1].trim().length() > 0) {
                  return new Cities(Integer.parseInt(p[0]), p[1].trim());
               }
            } else if (clazz.getName().equals(Hotels.class.getName())) {
               // get all values: id,city_id,clicks,impressions,name,rating,stars
               if (p[1] != null && p[1].trim().length() > 0) {
                  return new Hotels(Integer.parseInt(p[0]), p[4].trim(), Integer.parseInt(p[5]), Integer.parseInt(p[6]), Integer.parseInt(p[1]),
                        Integer.parseInt(p[2]), Integer.parseInt(p[3]));
               }
            } else if (clazz.getName().equals(Advertiser.class.getName())) {
               if (p[1] != null && p[1].trim().length() > 0) {
                  // get advertiser id and advertiser name
                  return new Advertiser(Integer.parseInt(p[0]), p[1].trim());
               }
            } else if (clazz.getName().equals(HotelAdvertiser.class.getName())) {
               // get advertiser id and hotel id
               return new HotelAdvertiser(Integer.parseInt(p[0]), Integer.parseInt(p[1]));
            }
            return null;
         }).collect(Collectors.toList());
      }
      catch (IOException e) {
         e.printStackTrace();
      }
      return null;
   }

   /**
    * Helper Class for Hotel to get data.
    * Added in this class for laziness.
    */
   public class Hotels extends Hotel {

      private int cityId;
      private int clicks;
      private int impressions;

      public Hotels(int id, String name, int rating, int stars, int cityId, int clicks, int impressions) {
         super(id, name, rating, stars);
         this.cityId = cityId;
         this.clicks = clicks;
         this.impressions = impressions;
      }

      public Hotel getHotel() {
         return new Hotel(this.getId(), this.getName(), this.getRating(), this.getStars());
      }

      public int getCityId() {
         return cityId;
      }

      public void setCityId(int cityId) {
         this.cityId = cityId;
      }

      public int getClicks() {
         return clicks;
      }

      public void setClicks(int clicks) {
         this.clicks = clicks;
      }

      public int getImpressions() {
         return impressions;
      }

      public void setImpressions(int impressions) {
         this.impressions = impressions;
      }
   }

   /**
    * Helper class for getting HotelAdvertiser content
    * Added in this class for laziness.
    */
   private class HotelAdvertiser {

      private int advertiserId;
      private int hotelId;

      public HotelAdvertiser(int advertiserId, int hotelId) {
         this.advertiserId = advertiserId;
         this.hotelId = hotelId;
      }

      public int getAdvertiserId() {
         return advertiserId;
      }

      public void setAdvertiserId(int advertiserId) {
         this.advertiserId = advertiserId;
      }

      public int getHotelId() {
         return hotelId;
      }

      public void setHotelId(int hotelId) {
         this.hotelId = hotelId;
      }
   }

   /**
    * Helper class for getting cities content
    * Added in this class for laziness.
    */
   private class Cities {

      private int id;
      private String cityname;

      public Cities(int id, String cityname) {
         this.id = id;
         this.cityname = cityname;
      }

      public int getId() {
         return id;
      }

      public void setId(int id) {
         this.id = id;
      }

      public String getCityname() {
         return cityname;
      }

      public void setCityname(String cityname) {
         this.cityname = cityname;
      }
   }
}



