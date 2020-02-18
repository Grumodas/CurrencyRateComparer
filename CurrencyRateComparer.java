import java.io.*;
import java.net.*;
import javax.xml.parsers.*;
import org.w3c.dom.*;
import java.nio.charset.Charset;
import java.util.*;
import java.text.*;
import java.time.*;
import java.math.BigDecimal;

//when we run this program on command line
//it asks to input start date and end date (YYYY-MM-DD format),
//asks how many currency codes the user would like to be displayed.
//Then it asks to enter those codes (i.e. USD, GBP, etc.)

//If an example is needed please look at the picture in the github repository.

//Note: I noticed at the "Date" Java class is buggy (see code for more info)
//so in the future it's worth replacing it with something more reliable.
public class CurrencyRateComparer {
	public static void main(String[] args) {
		Scanner scanner = new Scanner(System.in);

		NodeList nodesStart;
		while (true) {
			System.out.println("Įrašykite pradžios datą");
			String startDate = checkDate();

			try {
				nodesStart = connectToLB(startDate);
			} catch (Exception e) {
				System.out.println("----\nERROR -- "
				+ "This date is unavailable (probably weekend or holiday). Try again!\n----");
				continue;
			}		
			break;
		}

		NodeList nodesEnd;
		while (true) {
			System.out.println("Įrašykite pabaigos datą");
			String startDate = checkDate();

			try {
				nodesEnd = connectToLB(startDate);
			} catch (Exception e) {
				System.out.println("----\nERROR -- "
				+ "This date is unavailable (probably weekend or holiday). Try again!\n----");
				continue;
			}		
			break;
		}
		
		System.out.print("Kiek valiutų norėtumėte matyti: ");
		int noOfCurrencyCodes = Integer.parseInt(scanner.nextLine());

		ArrayList<String> currencyCodeList = new ArrayList<String>();
		String currencyCode;
		for (int i = 0; i < noOfCurrencyCodes; i++) {
			System.out.print("Įrašykite valiutos kodą: ");
			currencyCode = scanner.nextLine();
			currencyCode = currencyCode.toUpperCase();
			currencyCodeList.add(currencyCode);
		}
		BigDecimal differenceInRates[] = new BigDecimal[noOfCurrencyCodes];
		
		//iterates through the XML list and looks for the currencies specified
		//and adds their values to an array. 
		//---
		//when refactoring it's worth making this loop a function because
		//it's used twice (once for start date and once for end date)
		System.out.println("\n---------------");
		System.out.println("START DATA: ");
		for (int i = 0, importantCurrencyIndex = 0; i < nodesStart.getLength(); i++) {  	
			Node node = nodesStart.item(i);  
			if (node.getNodeType() == Node.ELEMENT_NODE) {  
				Element element = (Element) node;  
				String whatWeNeed = element.getElementsByTagName("valiutos_kodas")
						.item(0).getTextContent();
				if (!currencyCodeList.contains(whatWeNeed)) {
					continue;
				}
		
				System.out.println("Pavadinimas: " 
						+ element.getElementsByTagName("pavadinimas")
						.item(0).getTextContent());  
				System.out.println("Valiutos kodas: " 
						+ element.getElementsByTagName("valiutos_kodas")
						.item(0).getTextContent());  
				System.out.println("Santykis: " 
						+ element.getElementsByTagName("santykis")
						.item(0).getTextContent());  
				System.out.println("Data: " 
						+ element.getElementsByTagName("data")
						.item(0).getTextContent());

				String temp = element.getElementsByTagName("santykis")
							.item(0).getTextContent().replace(",", ".");	
				differenceInRates[importantCurrencyIndex++] = new BigDecimal(Double.parseDouble(temp));		
				System.out.println();
			}
		}

		System.out.println("---------------");
		System.out.println("\nEND DATA: ");
		for (int i = 0, importantCurrencyIndex = 0; i < nodesEnd.getLength(); i++) {  
			Node node = nodesEnd.item(i);  
			if (node.getNodeType() == Node.ELEMENT_NODE) {  
				Element element = (Element) node;  
				String whatWeNeed = element.getElementsByTagName("valiutos_kodas")
						.item(0).getTextContent();
				if (!currencyCodeList.contains(whatWeNeed)) {
					continue;
				}

				System.out.println("Pavadinimas: " 
						+ element.getElementsByTagName("pavadinimas")
						.item(0).getTextContent());  
				System.out.println("Valiutos kodas: " 
						+ element.getElementsByTagName("valiutos_kodas")
						.item(0).getTextContent());  
				System.out.println("Santykis: " 
						+ element.getElementsByTagName("santykis")
						.item(0).getTextContent());  
				System.out.println("Data: " 
						+ element.getElementsByTagName("data")
						.item(0).getTextContent());

				String temp = element.getElementsByTagName("santykis")
							.item(0).getTextContent().replace(",", ".");	
				differenceInRates[importantCurrencyIndex] = differenceInRates[importantCurrencyIndex++]
							.subtract(new BigDecimal(Double.parseDouble(temp)));
				System.out.println();
			}  
		}

		System.out.println("---------------");
		
		System.out.println("RESULTS");
		System.out.println("Differences in currency rates:");
		for (int i = 0; i < noOfCurrencyCodes; i++) {
			String result = differenceInRates[i].toString();
			int indexOfPoint = result.indexOf(".");
			result = result.substring(0, indexOfPoint + 6);
			result = result.replace("-", "");
			System.out.println(
				currencyCodeList.get(i) + ": " + result
			);
		}
	}

	//check if date is correct
	//format, date must be before 2014-09-30 (creation of DB), cannot be later than today
	public static String checkDate() {
		Scanner scanner = new Scanner(System.in);
		SimpleDateFormat dateFormat = new SimpleDateFormat("YYYY-MM-DD"); 
		Date startDate, oldestPossibleDate, today = Calendar.getInstance().getTime();
		NodeList nodes;
		String dateString;
		while (true) {
			System.out.print("YYYY-MM-DD: ");
			dateString = scanner.nextLine();

			Date endDate;
			try {
				endDate = dateFormat.parse(dateString);
				oldestPossibleDate = dateFormat.parse("2014-09-30");
			} catch (ParseException pe) {
				//note: this doesn't catch all the parsing exceptions
				//for example: 3-3-3 is accepted for some reason
				//needs to be improved
				System.out.println("----\nERROR -- "
				+ "Incorrect format (YYYY-MM-DD). Try again!\n----");
				continue;
			}
			
			//The methods compareTo() and before/after()
			//of class "Date" don't work 100% properly
			//onodesStarty works with dates which are much earlier or much later
			//so in the future it's best to substitute class "Date"
			//for something different completely
			if (endDate.before(oldestPossibleDate)) {
				System.out.println("----\nERROR -- "
				+ "The date is too old! Earliest is 2014-09-30. Try again!\n----");
				continue;
			}

			if (endDate.after(today)) {
				System.out.println("----\nERROR -- "
				+ "Can't choose a day later than today. Try again!\n----");
				continue;
			}
			break;
		}

		return dateString;
	}

	//obtains the data in XML from lb.lt, 
	//throws an exception if that day is unavailable
	public static NodeList connectToLB(String date) throws Exception {
		URLConnection connection = new URL("https://www.lb.lt/lt/currency" 
				+ "/daylyexport/?xml=1&class=Eu&type=day&date_day=" + date)
				.openConnection();

		connection.setRequestProperty("User-Agent", "Mozilla/5.0 " 
			+ "(Windows NT 6.1; WOW64) AppleWebKit/537.11 (KHTML, like Gecko) "
			+ "Chrome/23.0.1271.95 Safari/537.11");
		connection.connect();

		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = factory.newDocumentBuilder();
		Document doc = builder.parse(connection.getInputStream());
		NodeList nodesStart = doc.getElementsByTagName("item");  		

		if (nodesStart.getLength() == 1) {
			throw new Exception();
		}
		return nodesStart;
	}
}