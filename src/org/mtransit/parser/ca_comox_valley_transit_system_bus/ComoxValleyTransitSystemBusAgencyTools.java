package org.mtransit.parser.ca_comox_valley_transit_system_bus;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.mtransit.parser.CleanUtils;
import org.mtransit.parser.DefaultAgencyTools;
import org.mtransit.parser.Pair;
import org.mtransit.parser.SplitUtils;
import org.mtransit.parser.SplitUtils.RouteTripSpec;
import org.mtransit.parser.Utils;
import org.mtransit.parser.gtfs.data.GCalendar;
import org.mtransit.parser.gtfs.data.GCalendarDate;
import org.mtransit.parser.gtfs.data.GRoute;
import org.mtransit.parser.gtfs.data.GSpec;
import org.mtransit.parser.gtfs.data.GStop;
import org.mtransit.parser.gtfs.data.GTrip;
import org.mtransit.parser.gtfs.data.GTripStop;
import org.mtransit.parser.mt.data.MAgency;
import org.mtransit.parser.mt.data.MDirectionType;
import org.mtransit.parser.mt.data.MRoute;
import org.mtransit.parser.mt.data.MTrip;
import org.mtransit.parser.mt.data.MTripStop;

// https://bctransit.com/*/footer/open-data
// https://bctransit.com/servlet/bctransit/data/GTFS - Comox Valley
public class ComoxValleyTransitSystemBusAgencyTools extends DefaultAgencyTools {

	public static void main(String[] args) {
		if (args == null || args.length == 0) {
			args = new String[3];
			args[0] = "input/gtfs.zip";
			args[1] = "../../mtransitapps/ca-comox-valley-transit-system-bus-android/res/raw/";
			args[2] = ""; // files-prefix
		}
		new ComoxValleyTransitSystemBusAgencyTools().start(args);
	}

	private HashSet<String> serviceIds;

	@Override
	public void start(String[] args) {
		System.out.printf("\nGenerating Comox Valley Transit System bus data...");
		long start = System.currentTimeMillis();
		this.serviceIds = extractUsefulServiceIds(args, this, true);
		super.start(args);
		System.out.printf("\nGenerating Comox Valley Transit System bus data... DONE in %s.\n", Utils.getPrettyDuration(System.currentTimeMillis() - start));
	}

	@Override
	public boolean excludingAll() {
		return this.serviceIds != null && this.serviceIds.isEmpty();
	}

	private static final String INCLUDE_ONLY_SERVICE_ID_CONTAINS = null;

	@Override
	public boolean excludeCalendar(GCalendar gCalendar) {
		if (INCLUDE_ONLY_SERVICE_ID_CONTAINS != null && !gCalendar.getServiceId().contains(INCLUDE_ONLY_SERVICE_ID_CONTAINS)) {
			return true;
		}
		if (this.serviceIds != null) {
			return excludeUselessCalendar(gCalendar, this.serviceIds);
		}
		return super.excludeCalendar(gCalendar);
	}

	@Override
	public boolean excludeCalendarDate(GCalendarDate gCalendarDates) {
		if (INCLUDE_ONLY_SERVICE_ID_CONTAINS != null && !gCalendarDates.getServiceId().contains(INCLUDE_ONLY_SERVICE_ID_CONTAINS)) {
			return true;
		}
		if (this.serviceIds != null) {
			return excludeUselessCalendarDate(gCalendarDates, this.serviceIds);
		}
		return super.excludeCalendarDate(gCalendarDates);
	}

	private static final String INCLUDE_AGENCY_ID = "12"; // Comox Valley Transit System only

	@Override
	public boolean excludeRoute(GRoute gRoute) {
		if (StringUtils.isEmpty(gRoute.getAgencyId())) {
			return false; // no agency to discriminate
		}
		if (!INCLUDE_AGENCY_ID.equals(gRoute.getAgencyId())) {
			return true;
		}
		return super.excludeRoute(gRoute);
	}

	@Override
	public boolean excludeTrip(GTrip gTrip) {
		if (INCLUDE_ONLY_SERVICE_ID_CONTAINS != null && !gTrip.getServiceId().contains(INCLUDE_ONLY_SERVICE_ID_CONTAINS)) {
			return true;
		}
		if (this.serviceIds != null) {
			return excludeUselessTrip(gTrip, this.serviceIds);
		}
		return super.excludeTrip(gTrip);
	}

	@Override
	public Integer getAgencyRouteType() {
		return MAgency.ROUTE_TYPE_BUS;
	}

	@Override
	public long getRouteId(GRoute gRoute) {
		return Long.parseLong(gRoute.getRouteShortName()); // use route short name as route ID
	}

	@Override
	public String getRouteLongName(GRoute gRoute) {
		String routeLongName = gRoute.getRouteLongName();
		routeLongName = CleanUtils.cleanSlashes(routeLongName);
		routeLongName = CleanUtils.cleanNumbers(routeLongName);
		routeLongName = CleanUtils.cleanStreetTypes(routeLongName);
		return CleanUtils.cleanLabel(routeLongName);
	}

	private static final String AGENCY_COLOR_GREEN = "34B233";// GREEN (from PDF Corporate Graphic Standards)
	private static final String AGENCY_COLOR_BLUE = "002C77"; // BLUE (from PDF Corporate Graphic Standards)

	private static final String AGENCY_COLOR = AGENCY_COLOR_GREEN;

	@Override
	public String getAgencyColor() {
		return AGENCY_COLOR;
	}

	private static final String COLOR_004A8F = "004A8F";
	private static final String COLOR_7FB539 = "7FB539";
	private static final String COLOR_F78B1F = "F78B1F";
	private static final String COLOR_52A6ED = "52A6ED";
	private static final String COLOR_8E0C3A = "8E0C3A";
	private static final String COLOR_49176D = "49176D";
	private static final String COLOR_FCAF17 = "FCAF17";
	private static final String COLOR_EC008C = "EC008C";
	private static final String COLOR_401A64 = "401A64";
	private static final String COLOR_AB5C3B = "AB5C3B";
	private static final String COLOR_A3238E = "A3238E";
	private static final String COLOR_B2A97E = "B2A97E";
	private static final String COLOR_7C3F24 = "7C3F24";
	private static final String COLOR_00AA4F = "00AA4F";

	@Override
	public String getRouteColor(GRoute gRoute) {
		if (StringUtils.isEmpty(gRoute.getRouteColor())) {
			int rsn = Integer.parseInt(gRoute.getRouteShortName());
			switch (rsn) {
			// @formatter:off
			case 1: return COLOR_004A8F;
			case 2: return COLOR_7FB539;
			case 3: return COLOR_F78B1F;
			case 4: return COLOR_52A6ED;
			case 5: return COLOR_8E0C3A;
			case 6: return COLOR_49176D;
			case 7: return COLOR_FCAF17;
			case 8: return COLOR_EC008C;
			case 9: return COLOR_401A64;
			case 10: return COLOR_AB5C3B;
			case 11: return COLOR_A3238E;
			case 12: return COLOR_B2A97E;
			case 13: return null; // TODO
			case 14: return null; // TODO
			case 34: return COLOR_7C3F24;
			case 99: return COLOR_00AA4F;
			// @formatter:on
			default:
				if (isGoodEnoughAccepted()) {
					return AGENCY_COLOR_BLUE;
				}
				System.out.printf("\n%s: Unexpected route color for %s!\n", gRoute.getRouteId(), gRoute);
				System.exit(-1);
				return null;
			}
		}
		return super.getRouteColor(gRoute);
	}

	private static final String AIRPORT = "Airport";
	private static final String BUCKLEY_BAY = "Buckley Bay";
	private static final String COMOX = "Comox";
	private static final String CUMBERLAND = "Cumberland";
	private static final String DOWNTOWN = "Downtown";
	private static final String DRIFTWOOD_MALL = "Driftwood Mall";
	private static final String OYSTER_RIVER = "Oyster River";
	private static final String ROYSTON = "Royston";
	private static final String VANIER = "Vanier";
	private static final String COURTENAY = "Courtenay";
	private static final String SCHOOLS = "Schools";

	private static HashMap<Long, RouteTripSpec> ALL_ROUTE_TRIPS2;
	static {
		HashMap<Long, RouteTripSpec> map2 = new HashMap<Long, RouteTripSpec>();
		map2.put(3L, new RouteTripSpec(3L, //
				0, MTrip.HEADSIGN_TYPE_STRING, DOWNTOWN, // COURTENAY, //
				1, MTrip.HEADSIGN_TYPE_STRING, COMOX) //
				.addTripSort(0, //
						Arrays.asList(new String[] { //
						"111316", // Southbound Torrence at Ridgemount
								"111278", // Northbound Fitzgerald at 26th St
								"111270", // Downtown Exchange Bay B
						})) //
				.addTripSort(1, //
						Arrays.asList(new String[] { //
						"111270", // Downtown Exchange Bay B
								"111304", // Eastbound Guthrie at Stadacona
								"111315", // Eastbound Guthrie at Skeena
								"111316", // Southbound Torrence at Ridgemount
						})) //
				.compileBothTripSort());
		map2.put(4L, new RouteTripSpec(4L, //
				0, MTrip.HEADSIGN_TYPE_STRING, DOWNTOWN, // COURTENAY, //
				1, MTrip.HEADSIGN_TYPE_STRING, COMOX) //
				.addTripSort(0, //
						Arrays.asList(new String[] { //
						"111341", // Rodello at Fairbairn
								"111350", // Eastbound Comox at Nordin
								"111358", // == Westbound Guthrie at Skeena
								"134008", // !== Westbound Guthrie at Pritchard
								"111359", // !== Southbound Pritchard at Maquinna
								"111366", // != Northbound Church at Hemlock
								"111369", // !== Northbound Anderton at Guthrie
								"111370", // == Westbound Guthrie at Stadacona
								"111375", // == Northbound Lerwick at Valley View
								"111478", // != Northbound Lerwick at Malahat
								"111377", // == Northbound 470 block Lerwick
								"111448", // != Eastbound 3070 block Ryan =>
								"134010", // != Westbound Colby at Lerwick
								"111270", // Downtown Exchange Bay B =>
						})) //
				.addTripSort(1, //
						Arrays.asList(new String[] { //
						"111270", // Downtown Exchange Bay B
								"111278", // Northbound Fitzgerald at 26th St
								"111340", // Eastbound Comox at St Joseph Hospital
								"111341", // Rodello at Fairbairn
						})) //
				.compileBothTripSort());
		map2.put(5L, new RouteTripSpec(5L, //
				0, MTrip.HEADSIGN_TYPE_STRING, VANIER, //
				1, MTrip.HEADSIGN_TYPE_STRING, DOWNTOWN) //
				.addTripSort(0, //
						Arrays.asList(new String[] { //
						"111270", // Downtown Exchange Bay B <=
								"111296", // !=
								"111278", // != Northbound Fitzgerald at 26th St <=
								"111337", // !=
								"110270", // ==
								"110526", // Comox Valley Sports Centre
						})) //
				.addTripSort(1, //
						Arrays.asList(new String[] { //
						"110526", // Comox Valley Sports Centre
								"111380", // ++
								"111270", // Downtown Exchange Bay B
						})) //
				.compileBothTripSort());
		map2.put(6L, new RouteTripSpec(6L, //
				0, MTrip.HEADSIGN_TYPE_STRING, "NIC", //
				1, MTrip.HEADSIGN_TYPE_STRING, DOWNTOWN) //
				.addTripSort(0, //
						Arrays.asList(new String[] { //
						"111270", // Downtown Exchange Bay B
								"111385", // ++
								"111299", // Northbound College Campus
						})) //
				.addTripSort(1, //
						Arrays.asList(new String[] { //
						"111299", // Northbound College Campus
								"111463", // ++
								"111270", // Downtown Exchange Bay B
						})) //
				.compileBothTripSort());
		map2.put(7L, new RouteTripSpec(7L, //
				0, MTrip.HEADSIGN_TYPE_STRING, DRIFTWOOD_MALL, //
				1, MTrip.HEADSIGN_TYPE_STRING, DOWNTOWN) //
				.addTripSort(0, //
						Arrays.asList(new String[] { //
						"111270", // Downtown Exchange Bay B
								"111457", // ++
								"111405", // == Eastbound Woods at Martin
								"134003", // != Northbound 5th Street at Pidcock >> DOWNTOWN
								"111406", // != Southbound 5th St at Willemar
								"111278", // Northbound Fitzgerald at 26th St
						})) //
				.addTripSort(1, //
						Arrays.asList(new String[] { //
						"111278", // Northbound Fitzgerald at 26th St
								"111284", // != Northbound Fitzgerald at 10th St
								"134003", // != Northbound 5th Street at Pidcock
								"111432", // != Eastbound 5th St at Kilpatrick
								"111286", // == Northbound Fitzgerald at 5th St
								"111270", // Downtown Exchange Bay B
						})) //
				.compileBothTripSort());
		map2.put(12L, new RouteTripSpec(12L, //
				0, MTrip.HEADSIGN_TYPE_STRING, OYSTER_RIVER, //
				1, MTrip.HEADSIGN_TYPE_STRING, DOWNTOWN) // COURTENAY) //
				.addTripSort(0, //
						Arrays.asList(new String[] { //
						"111270", // Downtown Exchange Bay B
								"111296", // ==
								"110270", // !=
								"111492", // ==
								"134021", // Westbound Glenmore at Lambeth
						})) //
				.addTripSort(1, //
						Arrays.asList(new String[] { //
						"134021", // Westbound Glenmore at Lambeth
								"103860", // ==
								"110413", // !=
								"103861", // ==
								"134016", // ==
								"110526", // !=
								"110227", // !=
								"111379", // !=
								"111380", // ==
								"111270", // Downtown Exchange Bay B
						})) //
				.compileBothTripSort());
		map2.put(13L, new RouteTripSpec(13L, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, "Merville", //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, DOWNTOWN) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { //
						"111270", // Downtown Exchange Bay B
								"111299", // ++
								"110448", // Merville Rd Farside Island Hwy
						})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { //
						"110448", // Merville Rd Farside Island Hwy
								"111380", // ++
								"111270", // Downtown Exchange Bay B
						})) //
				.compileBothTripSort());
		map2.put(14L, new RouteTripSpec(14L, //
				0, MTrip.HEADSIGN_TYPE_STRING, DOWNTOWN, //
				1, MTrip.HEADSIGN_TYPE_STRING, "Union Bay") //
				.addTripSort(0, //
						Arrays.asList(new String[] { //
						"103863", // Northbound Island Hwy S at McLeod
								"111270", // Downtown Exchange Bay B
						})) //
				.addTripSort(1, //
						Arrays.asList(new String[] { //
						"111270", // Downtown Exchange Bay B
								"111435", // Southbound Island Hwy S at Russell
						})) //
				.compileBothTripSort());
		map2.put(34L, new RouteTripSpec(34L, //
				0, MTrip.HEADSIGN_TYPE_STRING, DOWNTOWN, // COURTENAY, //
				1, MTrip.HEADSIGN_TYPE_STRING, COMOX) //
				.addTripSort(0, //
						Arrays.asList(new String[] { //
						"111323", // Westbound Comox at Nordin
								"111371", // Guthrie at Aspen
								"111270", // Downtown Exchange Bay B
						})) //
				.addTripSort(1, //
						Arrays.asList(new String[] { //
						"111270", // Downtown Exchange Bay B
								"111300", // ++
								"111350", // Eastbound Comox at Nordin
						})) //
				.compileBothTripSort());
		map2.put(99L, new RouteTripSpec(99L, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, SCHOOLS, // AM
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, DOWNTOWN) // PM
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { //
						/* no stops */
						})) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { //
						"103874", // E Ryan at Little River
								"111270", // Downtown Exchange
						})) //
				.compileBothTripSort());
		ALL_ROUTE_TRIPS2 = map2;
	}

	@Override
	public int compareEarly(long routeId, List<MTripStop> list1, List<MTripStop> list2, MTripStop ts1, MTripStop ts2, GStop ts1GStop, GStop ts2GStop) {
		if (ALL_ROUTE_TRIPS2.containsKey(routeId)) {
			return ALL_ROUTE_TRIPS2.get(routeId).compare(routeId, list1, list2, ts1, ts2, ts1GStop, ts2GStop, this);
		}
		return super.compareEarly(routeId, list1, list2, ts1, ts2, ts1GStop, ts2GStop);
	}

	@Override
	public ArrayList<MTrip> splitTrip(MRoute mRoute, GTrip gTrip, GSpec gtfs) {
		if (ALL_ROUTE_TRIPS2.containsKey(mRoute.getId())) {
			return ALL_ROUTE_TRIPS2.get(mRoute.getId()).getAllTrips();
		}
		return super.splitTrip(mRoute, gTrip, gtfs);
	}

	@Override
	public Pair<Long[], Integer[]> splitTripStop(MRoute mRoute, GTrip gTrip, GTripStop gTripStop, ArrayList<MTrip> splitTrips, GSpec routeGTFS) {
		if (ALL_ROUTE_TRIPS2.containsKey(mRoute.getId())) {
			return SplitUtils.splitTripStop(mRoute, gTrip, gTripStop, routeGTFS, ALL_ROUTE_TRIPS2.get(mRoute.getId()), this);
		}
		return super.splitTripStop(mRoute, gTrip, gTripStop, splitTrips, routeGTFS);
	}

	@Override
	public void setTripHeadsign(MRoute mRoute, MTrip mTrip, GTrip gTrip, GSpec gtfs) {
		if (ALL_ROUTE_TRIPS2.containsKey(mRoute.getId())) {
			return; // split
		}
		mTrip.setHeadsignString(cleanTripHeadsign(gTrip.getTripHeadsign()), gTrip.getDirectionId());
	}

	@Override
	public boolean mergeHeadsign(MTrip mTrip, MTrip mTripToMerge) {
		List<String> headsignsValues = Arrays.asList(mTrip.getHeadsignValue(), mTripToMerge.getHeadsignValue());
		if (mTrip.getRouteId() == 2L) {
			if (Arrays.asList( //
					DRIFTWOOD_MALL, // ==
					DOWNTOWN // ==
					).containsAll(headsignsValues)) {
				mTrip.setHeadsignString(DOWNTOWN, mTrip.getHeadsignId());
				return true;
			} else if (Arrays.asList( //
					DRIFTWOOD_MALL, // ==
					DOWNTOWN, // ==
					ROYSTON, //
					BUCKLEY_BAY, //
					CUMBERLAND //
					).containsAll(headsignsValues)) {
				mTrip.setHeadsignString(CUMBERLAND, mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 10L) {
			if (Arrays.asList( //
					CUMBERLAND, // ==
					BUCKLEY_BAY //
					).containsAll(headsignsValues)) {
				mTrip.setHeadsignString(BUCKLEY_BAY, mTrip.getHeadsignId());
				return true;
			} else if (Arrays.asList( //
					CUMBERLAND, // ==
					DRIFTWOOD_MALL, //
					DOWNTOWN, //
					COURTENAY // ++
					).containsAll(headsignsValues)) {
				mTrip.setHeadsignString(COURTENAY, mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 11L) {
			if (Arrays.asList( //
					DOWNTOWN, // ==
					"Little River - P. R. Ferry - Airport", //
					AIRPORT // ++
					).containsAll(headsignsValues)) {
				mTrip.setHeadsignString(AIRPORT, mTrip.getHeadsignId());
				return true;
			}
		}
		System.out.printf("\n%s: Unexpected trips to merge: %s & %s!\n", mTrip.getRouteId(), mTrip, mTripToMerge);
		System.exit(-1);
		return false;
	}

	private static final String EXCH = "Exch";
	private static final Pattern EXCHANGE = Pattern.compile("((^|\\W){1}(exchange)(\\W|$){1})", Pattern.CASE_INSENSITIVE);
	private static final String EXCHANGE_REPLACEMENT = "$2" + EXCH + "$4";

	private static final Pattern STARTS_WITH_NUMBER = Pattern.compile("(^[\\d]+[\\S]*)", Pattern.CASE_INSENSITIVE);

	private static final Pattern ENDS_WITH_VIA = Pattern.compile("( via .*$)", Pattern.CASE_INSENSITIVE);
	private static final Pattern STARTS_WITH_TO = Pattern.compile("(^.* to )", Pattern.CASE_INSENSITIVE);

	private static final Pattern AND = Pattern.compile("( and )", Pattern.CASE_INSENSITIVE);
	private static final String AND_REPLACEMENT = " & ";

	private static final Pattern CLEAN_P1 = Pattern.compile("[\\s]*\\([\\s]*");
	private static final String CLEAN_P1_REPLACEMENT = " (";
	private static final Pattern CLEAN_P2 = Pattern.compile("[\\s]*\\)[\\s]*");
	private static final String CLEAN_P2_REPLACEMENT = ") ";

	private static final Pattern DOWNTOWN_ = Pattern.compile("((^|\\W){1}(downtwon)(\\W|$){1})", Pattern.CASE_INSENSITIVE);
	private static final String DOWNTOWN_REPLACEMENT = "$2" + DOWNTOWN + "$4";

	@Override
	public String cleanTripHeadsign(String tripHeadsign) {
		if (Utils.isUppercaseOnly(tripHeadsign, true, true)) {
			tripHeadsign = tripHeadsign.toLowerCase(Locale.ENGLISH);
		}
		tripHeadsign = EXCHANGE.matcher(tripHeadsign).replaceAll(EXCHANGE_REPLACEMENT);
		tripHeadsign = DOWNTOWN_.matcher(tripHeadsign).replaceAll(DOWNTOWN_REPLACEMENT);
		tripHeadsign = ENDS_WITH_VIA.matcher(tripHeadsign).replaceAll(StringUtils.EMPTY);
		tripHeadsign = STARTS_WITH_TO.matcher(tripHeadsign).replaceAll(StringUtils.EMPTY);
		tripHeadsign = AND.matcher(tripHeadsign).replaceAll(AND_REPLACEMENT);
		tripHeadsign = CLEAN_P1.matcher(tripHeadsign).replaceAll(CLEAN_P1_REPLACEMENT);
		tripHeadsign = CLEAN_P2.matcher(tripHeadsign).replaceAll(CLEAN_P2_REPLACEMENT);
		tripHeadsign = STARTS_WITH_NUMBER.matcher(tripHeadsign).replaceAll(StringUtils.EMPTY);
		tripHeadsign = CleanUtils.cleanStreetTypes(tripHeadsign);
		tripHeadsign = CleanUtils.cleanNumbers(tripHeadsign);
		return CleanUtils.cleanLabel(tripHeadsign);
	}

	private static final Pattern STARTS_WITH_BOUND = Pattern.compile("(^(east|west|north|south)bound)", Pattern.CASE_INSENSITIVE);

	@Override
	public String cleanStopName(String gStopName) {
		gStopName = STARTS_WITH_BOUND.matcher(gStopName).replaceAll(StringUtils.EMPTY);
		gStopName = CleanUtils.CLEAN_AT.matcher(gStopName).replaceAll(CleanUtils.CLEAN_AT_REPLACEMENT);
		gStopName = EXCHANGE.matcher(gStopName).replaceAll(EXCHANGE_REPLACEMENT);
		gStopName = CleanUtils.cleanStreetTypes(gStopName);
		gStopName = CleanUtils.cleanNumbers(gStopName);
		return CleanUtils.cleanLabel(gStopName);
	}
}
