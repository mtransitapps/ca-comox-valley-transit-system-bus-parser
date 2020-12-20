package org.mtransit.parser.ca_comox_valley_transit_system_bus;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.mtransit.commons.StrategicMappingCommons;
import org.mtransit.parser.CleanUtils;
import org.mtransit.parser.DefaultAgencyTools;
import org.mtransit.parser.MTLog;
import org.mtransit.parser.Pair;
import org.mtransit.parser.SplitUtils;
import org.mtransit.parser.SplitUtils.RouteTripSpec;
import org.mtransit.parser.StringUtils;
import org.mtransit.parser.Utils;
import org.mtransit.parser.gtfs.data.GCalendar;
import org.mtransit.parser.gtfs.data.GCalendarDate;
import org.mtransit.parser.gtfs.data.GRoute;
import org.mtransit.parser.gtfs.data.GSpec;
import org.mtransit.parser.gtfs.data.GStop;
import org.mtransit.parser.gtfs.data.GTrip;
import org.mtransit.parser.gtfs.data.GTripStop;
import org.mtransit.parser.mt.data.MAgency;
import org.mtransit.parser.mt.data.MRoute;
import org.mtransit.parser.mt.data.MTrip;
import org.mtransit.parser.mt.data.MTripStop;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.mtransit.parser.Constants.EMPTY;

// https://www.bctransit.com/open-data
// https://comox.mapstrat.com/current/google_transit.zip
public class ComoxValleyTransitSystemBusAgencyTools extends DefaultAgencyTools {

	public static void main(@Nullable String[] args) {
		if (args == null || args.length == 0) {
			args = new String[3];
			args[0] = "input/gtfs.zip";
			args[1] = "../../mtransitapps/ca-comox-valley-transit-system-bus-android/res/raw/";
			args[2] = ""; // files-prefix
		}
		new ComoxValleyTransitSystemBusAgencyTools().start(args);
	}

	@Nullable
	private HashSet<Integer> serviceIdInts;

	@Override
	public void start(@NotNull String[] args) {
		MTLog.log("Generating Comox Valley Transit System bus data...");
		long start = System.currentTimeMillis();
		this.serviceIdInts = extractUsefulServiceIdInts(args, this, true);
		super.start(args);
		MTLog.log("Generating Comox Valley Transit System bus data... DONE in %s.", Utils.getPrettyDuration(System.currentTimeMillis() - start));
	}

	@Override
	public boolean excludingAll() {
		return this.serviceIdInts != null && this.serviceIdInts.isEmpty();
	}

	@Override
	public boolean excludeCalendar(@NotNull GCalendar gCalendar) {
		if (this.serviceIdInts != null) {
			return excludeUselessCalendarInt(gCalendar, this.serviceIdInts);
		}
		return super.excludeCalendar(gCalendar);
	}

	@Override
	public boolean excludeCalendarDate(@NotNull GCalendarDate gCalendarDates) {
		if (this.serviceIdInts != null) {
			return excludeUselessCalendarDateInt(gCalendarDates, this.serviceIdInts);
		}
		return super.excludeCalendarDate(gCalendarDates);
	}

	@Override
	public boolean excludeTrip(@NotNull GTrip gTrip) {
		if (this.serviceIdInts != null) {
			return excludeUselessTripInt(gTrip, this.serviceIdInts);
		}
		return super.excludeTrip(gTrip);
	}

	@NotNull
	@Override
	public Integer getAgencyRouteType() {
		return MAgency.ROUTE_TYPE_BUS;
	}

	private static final Pattern DIGITS = Pattern.compile("[\\d]+");

	@Override
	public long getRouteId(@NotNull GRoute gRoute) { // used by GTFS-RT
		return super.getRouteId(gRoute);
	}

	@Nullable
	@Override
	public String getRouteShortName(@NotNull GRoute gRoute) {
		Matcher matcher = DIGITS.matcher(gRoute.getRouteShortName());
		if (matcher.find()) {
			return matcher.group(); // merge routes
		}
		throw new MTLog.Fatal("Unexpected route short name %s!", gRoute);
	}

	@Override
	public boolean mergeRouteLongName(@NotNull MRoute mRoute, @NotNull MRoute mRouteToMerge) {
		final long rsn = Long.parseLong(mRoute.getShortNameOrDefault());
		if (rsn == 1L) {
			mRoute.setLongName("Comox Mall / Anfield Ctr Via N.I.C.");
			return true;
		}
		throw new MTLog.Fatal("Unexpected routes long name to merge: %s & %s!", mRoute, mRouteToMerge);
	}

	@NotNull
	@Override
	public String getRouteLongName(@NotNull GRoute gRoute) {
		String routeLongName = gRoute.getRouteLongNameOrDefault();
		routeLongName = CleanUtils.cleanSlashes(routeLongName);
		routeLongName = CleanUtils.cleanNumbers(routeLongName);
		routeLongName = CleanUtils.cleanStreetTypes(routeLongName);
		return CleanUtils.cleanLabel(routeLongName);
	}

	private static final String AGENCY_COLOR_GREEN = "34B233";// GREEN (from PDF Corporate Graphic Standards)
	// private static final String AGENCY_COLOR_BLUE = "002C77"; // BLUE (from PDF Corporate Graphic Standards)

	private static final String AGENCY_COLOR = AGENCY_COLOR_GREEN;

	@NotNull
	@Override
	public String getAgencyColor() {
		return AGENCY_COLOR;
	}

	@Nullable
	@Override
	public String getRouteColor(@NotNull GRoute gRoute) {
		if (StringUtils.isEmpty(gRoute.getRouteColor())) {
			int rsn = Integer.parseInt(gRoute.getRouteShortName());
			switch (rsn) {
			// @formatter:off
			case 1: return "004A8F";
			case 2: return "7FB539";
			case 3: return "F78B1F";
			case 4: return "52A6ED";
			case 5: return "8E0C3A";
			case 6: return "49176D";
			case 7: return "FCAF17";
			case 8: return "EC008C";
			case 9: return "401A64";
			case 10: return "AB5C3B";
			case 11: return "A3238E";
			case 12: return "B2A97E";
			case 34: return "7C3F24";
			case 99: return "00AA4F";
			// @formatter:on
			default:
				throw new MTLog.Fatal("%s: Unexpected route color for %s!", gRoute.getRouteShortName(), gRoute);
			}
		}
		return super.getRouteColor(gRoute);
	}

	private static final long ROUTE_ID_0 = 354L;

	private static final HashMap<Long, RouteTripSpec> ALL_ROUTE_TRIPS2;

	static {
		HashMap<Long, RouteTripSpec> map2 = new HashMap<>();
		//noinspection deprecation
		map2.put(ROUTE_ID_0 + 5L, new RouteTripSpec(ROUTE_ID_0 + 5L, // 5 // SPLITTED FROM 1 DIRECTION
				StrategicMappingCommons.NORTH, MTrip.HEADSIGN_TYPE_STRING, "Comox Valley Sports Ctr", //
				StrategicMappingCommons.SOUTH, MTrip.HEADSIGN_TYPE_STRING, "Downtown Courtenay") //
				.addTripSort(StrategicMappingCommons.NORTH, //
						Arrays.asList(//
								Stops.getALL_STOPS().get("111486"), // Downtown Exchange Bay A <=
								Stops.getALL_STOPS().get("111296"), // !=
								Stops.getALL_STOPS().get("111278"), // != Northbound Fitzgerald at 26th St <=
								Stops.getALL_STOPS().get("111337"), // !=
								Stops.getALL_STOPS().get("110270"), // ==
								Stops.getALL_STOPS().get("110526") // Comox Valley Sports Centre
						)) //
				.addTripSort(StrategicMappingCommons.SOUTH, //
						Arrays.asList(//
								Stops.getALL_STOPS().get("110526"), // Comox Valley Sports Centre (NB)
								Stops.getALL_STOPS().get("111380"), // ++
								Stops.getALL_STOPS().get("111486") // Downtown Exchange Bay A
						)) //
				.compileBothTripSort());
		//noinspection deprecation
		map2.put(ROUTE_ID_0 + 6L, new RouteTripSpec(ROUTE_ID_0 + 6L, // 6 // SPLITTED FROM 1 DIRECTION
				StrategicMappingCommons.COUNTERCLOCKWISE_0, MTrip.HEADSIGN_TYPE_STRING, "NIC", //
				StrategicMappingCommons.COUNTERCLOCKWISE_1, MTrip.HEADSIGN_TYPE_STRING, "Downtown") //
				.addTripSort(StrategicMappingCommons.COUNTERCLOCKWISE_0, //
						Arrays.asList(//
								Stops.getALL_STOPS().get("111486"), // != Downtown Exchange Bay A <=
								Stops.getALL_STOPS().get("111297"), // != Ryan at Puntledge
								Stops.getALL_STOPS().get("111379"), // != Ryan at Puntledge <=
								Stops.getALL_STOPS().get("111298"), // == Ryan 1140 block
								Stops.getALL_STOPS().get("111385"), // ++ Sitka at E 10th St (SB)
								Stops.getALL_STOPS().get("111299") // NIC Campus Bay C (NB)
						)) //
				.addTripSort(StrategicMappingCommons.COUNTERCLOCKWISE_1, //
						Arrays.asList(//
								Stops.getALL_STOPS().get("111299"), // NIC Campus Bay C (NB)
								Stops.getALL_STOPS().get("111463"), // ++ McLauchlin at Dingwall (SB)
								Stops.getALL_STOPS().get("111379"), // == Ryan at Puntledge =>
								Stops.getALL_STOPS().get("111380"), // != Old Island at Puntledge
								Stops.getALL_STOPS().get("111486") // Downtown Exchange Bay A =>
						)) //
				.compileBothTripSort());
		//noinspection deprecation
		map2.put(ROUTE_ID_0 + 12L, new RouteTripSpec(ROUTE_ID_0 + 12L, // 13 // SPLITTED FROM 1 DIRECTION
				StrategicMappingCommons.COUNTERCLOCKWISE_0, MTrip.HEADSIGN_TYPE_STRING, "Merville", //
				StrategicMappingCommons.COUNTERCLOCKWISE_1, MTrip.HEADSIGN_TYPE_STRING, "Downtown") //
				.addTripSort(StrategicMappingCommons.COUNTERCLOCKWISE_0, //
						Arrays.asList(//
								Stops.getALL_STOPS().get("111486"), // Downtown Exchange Bay A
								Stops.getALL_STOPS().get("111299"), // ++ NIC Campus Bay C (NB)
								Stops.getALL_STOPS().get("110448") // Merville Rd Farside Island Hwy (WB)
						)) //
				.addTripSort(StrategicMappingCommons.COUNTERCLOCKWISE_1, //
						Arrays.asList(//
								Stops.getALL_STOPS().get("110448"), // Merville Rd Farside Island Hwy (WB)
								Stops.getALL_STOPS().get("111380"), // ++ Old Island at Puntledge (SB)
								Stops.getALL_STOPS().get("111486") // Downtown Exchange Bay A
						)) //
				.compileBothTripSort());
		//noinspection deprecation
		map2.put(ROUTE_ID_0 + 15L, new RouteTripSpec(ROUTE_ID_0 + 15L, // 99 // SPLITTED FROM 1 DIRECTION HEAD-SIGN
				StrategicMappingCommons.CLOCKWISE, MTrip.HEADSIGN_TYPE_STRING, "AM", // AM
				StrategicMappingCommons.COUNTERCLOCKWISE, MTrip.HEADSIGN_TYPE_STRING, "PM") // PM
				.addTripSort(StrategicMappingCommons.CLOCKWISE, //
						Arrays.asList( //
								Stops.getALL_STOPS().get("111486"), // xx <> Downtown Exchange Bay A
								Stops.getALL_STOPS().get("111381"), // != != 5th St 70 block
								Stops.getALL_STOPS().get("111375"), // != Lerwick at Valley View
								Stops.getALL_STOPS().get("111479"), // xx <> Isfield Secondary
								Stops.getALL_STOPS().get("111478"), // xx Lerwick at Malahat
								Stops.getALL_STOPS().get("111377"), // xx Lerwick 470 block
								Stops.getALL_STOPS().get("110248"), // != Lerwick FS Waters Pl
								Stops.getALL_STOPS().get("111390"), // <> Mission at Walbran
								Stops.getALL_STOPS().get("111391"), // <> ?? Mission at Shetland
								Stops.getALL_STOPS().get("111392"), // <> ?? Muir at Anna
								Stops.getALL_STOPS().get("111393"), // <> ?? Muir at Cruickshank
								Stops.getALL_STOPS().get("111394"), // <> ?? McLauchlin at MacIntyre
								Stops.getALL_STOPS().get("111463"), // <> ?? McLauchlin at Dingwall
								Stops.getALL_STOPS().get("111395"), // <> ?? McLauchlin at Panorama
								Stops.getALL_STOPS().get("111396"), // <> ?? Centennial at Back
								Stops.getALL_STOPS().get("111384"), // <> ?? Back at Tunner
								Stops.getALL_STOPS().get("111456"), // <> ?? Back at E 6th St
								Stops.getALL_STOPS().get("111473"), // <> ?? Back at E 10th St
								Stops.getALL_STOPS().get("111455"), // <> ?? E 10th St at Glen Urquhart
								Stops.getALL_STOPS().get("111385"), // <> ?? Sitka at E 10th St
								Stops.getALL_STOPS().get("111386"), // <> ?? Sitka at Malahat
								Stops.getALL_STOPS().get("111387"), // <> ?? Thorpe at Malahat
								Stops.getALL_STOPS().get("111388"), // <> ?? Thorpe at Griffin
								Stops.getALL_STOPS().get("111389"), // <> ?? Valley View at Partridge
								Stops.getALL_STOPS().get("111481"), // <> Valley View at Lerwick
								Stops.getALL_STOPS().get("111479"), // <> xx Isfield Secondary
								Stops.getALL_STOPS().get("111478"), // != xx Lerwick at Malahat
								Stops.getALL_STOPS().get("111377"), // xx Lerwick 470 block
								Stops.getALL_STOPS().get("111492"), // != <> Vanier 2990 block
								Stops.getALL_STOPS().get("111380"), // == Old Island at Puntledge (SB)
								Stops.getALL_STOPS().get("111489"), // == ++ Anderton at 3rd St
								Stops.getALL_STOPS().get("111486") // xx <> Downtown Exchange Bay A
						)) //
				.addTripSort(StrategicMappingCommons.COUNTERCLOCKWISE, //
						Arrays.asList( //
								Stops.getALL_STOPS().get("111492"), // <> Vanier 2990 block
								Stops.getALL_STOPS().get("111390"), // <> Mission at Walbran
								Stops.getALL_STOPS().get("111390"), // <> Mission at Walbran
								Stops.getALL_STOPS().get("111391"), // <> ?? Mission at Shetland
								Stops.getALL_STOPS().get("111392"), // <> ?? Muir at Anna
								Stops.getALL_STOPS().get("111393"), // <> ?? Muir at Cruickshank
								Stops.getALL_STOPS().get("111394"), // <> ?? McLauchlin at MacIntyre
								Stops.getALL_STOPS().get("111463"), // <> ?? McLauchlin at Dingwall
								Stops.getALL_STOPS().get("111395"), // <> ?? McLauchlin at Panorama
								Stops.getALL_STOPS().get("111396"), // <> ?? Centennial at Back
								Stops.getALL_STOPS().get("111384"), // <> ?? Back at Tunner
								Stops.getALL_STOPS().get("111456"), // <> ?? Back at E 6th St
								Stops.getALL_STOPS().get("111473"), // <> ?? Back at E 10th St
								Stops.getALL_STOPS().get("111455"), // <> ?? E 10th St at Glen Urquhart
								Stops.getALL_STOPS().get("111385"), // <> ?? Sitka at E 10th St
								Stops.getALL_STOPS().get("111386"), // <> ?? Sitka at Malahat
								Stops.getALL_STOPS().get("111387"), // <> ?? Thorpe at Malahat
								Stops.getALL_STOPS().get("111388"), // <> ?? Thorpe at Griffin
								Stops.getALL_STOPS().get("111389"), // <> ?? Valley View at Partridge
								Stops.getALL_STOPS().get("111481"), // <> Valley View at Lerwick
								Stops.getALL_STOPS().get("111479"), // <> xx Isfield Secondary
								Stops.getALL_STOPS().get("111301"), // != Lerwick at Valley View
								Stops.getALL_STOPS().get("103874"), // E Ryan at Little River
								Stops.getALL_STOPS().get("111379"), // != Ryan at Puntledge (WB)
								Stops.getALL_STOPS().get("111380"), // == Old Island at Puntledge (SB)
								Stops.getALL_STOPS().get("111489"), // == ++ Anderton at 3rd St
								Stops.getALL_STOPS().get("111486") // == Downtown Exchange Bay A
						)) //
				.compileBothTripSort());
		ALL_ROUTE_TRIPS2 = map2;
	}

	@Override
	public int compareEarly(long routeId, @NotNull List<MTripStop> list1, @NotNull List<MTripStop> list2, @NotNull MTripStop ts1, @NotNull MTripStop ts2, @NotNull GStop ts1GStop, @NotNull GStop ts2GStop) {
		if (ALL_ROUTE_TRIPS2.containsKey(routeId)) {
			return ALL_ROUTE_TRIPS2.get(routeId).compare(routeId, list1, list2, ts1, ts2, ts1GStop, ts2GStop, this);
		}
		return super.compareEarly(routeId, list1, list2, ts1, ts2, ts1GStop, ts2GStop);
	}

	@NotNull
	@Override
	public ArrayList<MTrip> splitTrip(@NotNull MRoute mRoute, @Nullable GTrip gTrip, @NotNull GSpec gtfs) {
		if (ALL_ROUTE_TRIPS2.containsKey(mRoute.getId())) {
			return ALL_ROUTE_TRIPS2.get(mRoute.getId()).getAllTrips();
		}
		return super.splitTrip(mRoute, gTrip, gtfs);
	}

	@NotNull
	@Override
	public Pair<Long[], Integer[]> splitTripStop(@NotNull MRoute mRoute, @NotNull GTrip gTrip, @NotNull GTripStop gTripStop, @NotNull ArrayList<MTrip> splitTrips, @NotNull GSpec routeGTFS) {
		if (ALL_ROUTE_TRIPS2.containsKey(mRoute.getId())) {
			return SplitUtils.splitTripStop(mRoute, gTrip, gTripStop, routeGTFS, ALL_ROUTE_TRIPS2.get(mRoute.getId()), this);
		}
		return super.splitTripStop(mRoute, gTrip, gTripStop, splitTrips, routeGTFS);
	}

	private final HashMap<Long, Long> routeIdToShortName = new HashMap<>();

	@Override
	public void setTripHeadsign(@NotNull MRoute mRoute, @NotNull MTrip mTrip, @NotNull GTrip gTrip, @NotNull GSpec gtfs) {
		if (ALL_ROUTE_TRIPS2.containsKey(mRoute.getId())) {
			return; // split
		}
		final long rsn = Long.parseLong(mRoute.getShortNameOrDefault());
		this.routeIdToShortName.put(mRoute.getId(), rsn);
		//noinspection deprecation
		final String routeId = gTrip.getRouteId();
		final int directionId = gTrip.getDirectionIdOrDefault();
		if (rsn == 1L) {
			if (!"1".equalsIgnoreCase(routeId)) {
				if ("16".equalsIgnoreCase(routeId)) { // Comox Mall - EAST
					if (directionId == 1) {
						if ("Comox Mall Via N.I.C.".equalsIgnoreCase(gTrip.getTripHeadsign())) {
							mTrip.setHeadsignString(cleanTripHeadsign(gTrip.getTripHeadsign()), StrategicMappingCommons.EAST);
							return;
						}
					} else if (directionId == 0) {
						if ("Downtown".equalsIgnoreCase(gTrip.getTripHeadsign())) {
							mTrip.setHeadsignString(cleanTripHeadsign(gTrip.getTripHeadsign()), StrategicMappingCommons.EAST);
							return;
						}
					}
				} else if ("17".equalsIgnoreCase(routeId)) { // Anfield Ctr - WEST
					if (directionId == 1) {
						if ("Anfield Centre Via Downtown".equalsIgnoreCase(gTrip.getTripHeadsign())) {
							mTrip.setHeadsignString(cleanTripHeadsign(gTrip.getTripHeadsign()), StrategicMappingCommons.WEST);
							return;
						}
					} else if (directionId == 0) {
						if ("Downtown".equalsIgnoreCase(gTrip.getTripHeadsign())) {
							mTrip.setHeadsignString(cleanTripHeadsign(gTrip.getTripHeadsign()), StrategicMappingCommons.WEST);
							return;
						}
					}
				}
			}
			if (directionId == 1) { // Comox Mall - EAST
				if ("Comox Mall Via N.I.C.".equalsIgnoreCase(gTrip.getTripHeadsign()) //
						|| "Downtown".equalsIgnoreCase(gTrip.getTripHeadsign())) {
					mTrip.setHeadsignString(cleanTripHeadsign(gTrip.getTripHeadsign()), StrategicMappingCommons.EAST);
					return;
				}
			} else if (directionId == 0) { // Anfield Ctr - WEST
				if ("Anfield Centre Via Downtown".equalsIgnoreCase(gTrip.getTripHeadsign()) //
						|| "Downtown".equalsIgnoreCase(gTrip.getTripHeadsign())) {
					mTrip.setHeadsignString(cleanTripHeadsign(gTrip.getTripHeadsign()), StrategicMappingCommons.WEST);
					return;
				}
			}
		} else if (rsn == 2L) {
			if (directionId == 0) { // Anfield Ctr - NORTH
				if ("Anfield Centre".equalsIgnoreCase(gTrip.getTripHeadsign()) //
						|| "To Downtown".equalsIgnoreCase(gTrip.getTripHeadsign()) //
						|| "Driftwood Mall to 4 Comox Mall".equalsIgnoreCase(gTrip.getTripHeadsign())) {
					mTrip.setHeadsignString(cleanTripHeadsign(gTrip.getTripHeadsign()), StrategicMappingCommons.NORTH);
					return;
				}
			} else if (directionId == 1) { // Cumberland - SOUTH
				if ("Cumberland".equalsIgnoreCase(gTrip.getTripHeadsign())) {
					mTrip.setHeadsignString(cleanTripHeadsign(gTrip.getTripHeadsign()), StrategicMappingCommons.SOUTH);
					return;
				}
			}
		} else if (rsn == 3L) {
			if (directionId == 1) { //
				if ("Comox Local".equalsIgnoreCase(gTrip.getTripHeadsign()) //
						|| "Comox Local to Isfeld School".equalsIgnoreCase(gTrip.getTripHeadsign())) {
					mTrip.setHeadsignString(cleanTripHeadsign(gTrip.getTripHeadsign()), StrategicMappingCommons.COUNTERCLOCKWISE_1);
					return;
				}
			}
		} else if (rsn == 4L) {
			if (directionId == 1) { // Comox Mall - EAST
				if (EMPTY.equalsIgnoreCase(gTrip.getTripHeadsign())) { // FIXME
					mTrip.setHeadsignString(cleanTripHeadsign("Comox Mall"), StrategicMappingCommons.EAST);
					return;
				}
				if ("Comox Mall Via Comox Rd".equalsIgnoreCase(gTrip.getTripHeadsign())) {
					mTrip.setHeadsignString(cleanTripHeadsign(gTrip.getTripHeadsign()), StrategicMappingCommons.EAST);
					return;
				}
			} else if (directionId == 0) { // Driftwood Mall - WEST
				if ("Driftwood Mall Via Comox Rd".equalsIgnoreCase(gTrip.getTripHeadsign())) {
					mTrip.setHeadsignString(cleanTripHeadsign(gTrip.getTripHeadsign()), StrategicMappingCommons.WEST);
					return;
				}
			}
		} else if (rsn == 7L) {
			if (directionId == 1) { //
				if ("Arden".equalsIgnoreCase(gTrip.getTripHeadsign()) //
						|| "Arden to Driftwood Mall".equalsIgnoreCase(gTrip.getTripHeadsign())) {
					mTrip.setHeadsignString(cleanTripHeadsign(gTrip.getTripHeadsign()), StrategicMappingCommons.COUNTERCLOCKWISE_1);
					return;
				}
			}
		} else if (rsn == 8L) {
			if (directionId == 0) { // Downtown - NORTH
				if ("Downtown Via Willemar".equalsIgnoreCase(gTrip.getTripHeadsign())) {
					mTrip.setHeadsignString(cleanTripHeadsign(gTrip.getTripHeadsign()), StrategicMappingCommons.NORTH);
					return;
				}
			} else if (directionId == 1) { // Anfield Centre - SOUTH
				if ("Anfield Centre Via Willemar".equalsIgnoreCase(gTrip.getTripHeadsign())) {
					mTrip.setHeadsignString(cleanTripHeadsign(gTrip.getTripHeadsign()), StrategicMappingCommons.SOUTH);
					return;
				}
			}
		} else if (rsn == 10L) {
			if (directionId == 0) { // Downtown Courtenay - NORTH
				if ("Downtown Courtenay".equalsIgnoreCase(gTrip.getTripHeadsign()) //
						|| "Anfield Centre to 8 Downtown".equalsIgnoreCase(gTrip.getTripHeadsign())) {
					mTrip.setHeadsignString(cleanTripHeadsign(gTrip.getTripHeadsign()), StrategicMappingCommons.NORTH);
					return;
				}
			} else if (directionId == 1) { // Fanny Bay - SOUTH
				if ("Fanny Bay Via Royston".equalsIgnoreCase(gTrip.getTripHeadsign())) {
					mTrip.setHeadsignString(cleanTripHeadsign(gTrip.getTripHeadsign()), StrategicMappingCommons.SOUTH);
					return;
				}
			}
		} else if (rsn == 11L) {
			if (directionId == 1) { // Airport - EAST
				if ("Airport".equalsIgnoreCase(gTrip.getTripHeadsign()) //
						|| "Airport - Powell R. Ferry".equalsIgnoreCase(gTrip.getTripHeadsign())) {
					mTrip.setHeadsignString(cleanTripHeadsign(gTrip.getTripHeadsign()), StrategicMappingCommons.EAST);
					return;
				}
			} else if (directionId == 0) { // Downtown - WEST
				if ("Downtown".equalsIgnoreCase(gTrip.getTripHeadsign())) {
					mTrip.setHeadsignString(cleanTripHeadsign(gTrip.getTripHeadsign()), StrategicMappingCommons.WEST);
					return;
				}
			}
		} else if (rsn == 12L) {
			if (directionId == 0) { // Oyster River - NORTH
				if (EMPTY.equalsIgnoreCase(gTrip.getTripHeadsign())) { // FIXME
					mTrip.setHeadsignString(cleanTripHeadsign("Oyster River"), StrategicMappingCommons.NORTH);
					return;
				}
				if ("North Valley Connector".equalsIgnoreCase(gTrip.getTripHeadsign()) // <>
						|| "Oyster River Via Vanier".equalsIgnoreCase(gTrip.getTripHeadsign())) {
					mTrip.setHeadsignString(cleanTripHeadsign(gTrip.getTripHeadsign()), StrategicMappingCommons.NORTH);
					return;
				}
			} else if (directionId == 1) { // Downtown Courtenay - SOUTH
				if (EMPTY.equalsIgnoreCase(gTrip.getTripHeadsign())) { // FIXME
					mTrip.setHeadsignString(cleanTripHeadsign("Downtown Courtenay"), StrategicMappingCommons.SOUTH);
					return;
				}
				if ("North Valley Connector".equalsIgnoreCase(gTrip.getTripHeadsign()) // <>
						|| "Downtown Courtenay Via N.I.C.".equalsIgnoreCase(gTrip.getTripHeadsign()) //
						|| "Downtown Courtenay Via Vanier".equalsIgnoreCase(gTrip.getTripHeadsign())) {
					mTrip.setHeadsignString(cleanTripHeadsign(gTrip.getTripHeadsign()), StrategicMappingCommons.SOUTH);
					return;
				}
			}
		} else if (rsn == 14L) {
			if (directionId == 0) { // Downtown - NORTH
				if ("Downtown".equalsIgnoreCase(gTrip.getTripHeadsign())) {
					mTrip.setHeadsignString(cleanTripHeadsign(gTrip.getTripHeadsign()), StrategicMappingCommons.NORTH);
					return;
				}
			} else if (directionId == 1) { // Union Bay - SOUTH
				if ("Union Bay".equalsIgnoreCase(gTrip.getTripHeadsign())) {
					mTrip.setHeadsignString(cleanTripHeadsign(gTrip.getTripHeadsign()), StrategicMappingCommons.SOUTH);
					return;
				}
			}
		} else if (rsn == 20L) {
			if (directionId == 0) { //
				if ("Cumberland Via Royston".equalsIgnoreCase(gTrip.getTripHeadsign())) {
					mTrip.setHeadsignString(cleanTripHeadsign(gTrip.getTripHeadsign()), StrategicMappingCommons.WEST);
					return;
				}
			}
		}
		throw new MTLog.Fatal("%s: %s: Unexpected trips head-sign for %s!", rsn, mTrip.getRouteId(), gTrip.toStringPlus());
	}

	@Override
	public boolean directionFinderEnabled() {
		return false; // DISABLED because 99 is AM/PM...
	}

	@Override
	public boolean mergeHeadsign(@NotNull MTrip mTrip, @NotNull MTrip mTripToMerge) {
		List<String> headsignsValues = Arrays.asList(mTrip.getHeadsignValue(), mTripToMerge.getHeadsignValue());
		final long rsn = this.routeIdToShortName.get(mTrip.getRouteId());
		if (rsn == 1L) {
			if (Arrays.asList( //
					"Downtown", // <>
					"Anfield Ctr" //
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString("Anfield Ctr", mTrip.getHeadsignId());
				return true;
			}
			if (Arrays.asList( //
					"Downtown", // <>
					"Comox Mall" //
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString("Comox Mall", mTrip.getHeadsignId());
				return true;
			}
		} else if (rsn == 2L) {
			if (Arrays.asList( //
					"Comox Mall", //
					"Downtown", //
					"Anfield Ctr" //
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString("Anfield Ctr", mTrip.getHeadsignId());
				return true;
			}
		} else if (rsn == 3L) {
			if (Arrays.asList( //
					"Isfeld School", //
					"Comox Local" //
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString("Comox Local", mTrip.getHeadsignId());
				return true;
			}
		} else if (rsn == 7L) {
			if (Arrays.asList( //
					"Driftwood Mall", //
					"Arden" //
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString("Arden", mTrip.getHeadsignId());
				return true;
			}
		} else if (rsn == 10L) {
			if (Arrays.asList( //
					"Downtown", //
					"Downtown Courtenay" //
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString("Downtown Courtenay", mTrip.getHeadsignId());
				return true;
			}
		} else if (rsn == 11L) {
			if (Arrays.asList( //
					"Airport - Powell R Ferry", //
					"Airport" //
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString("Airport", mTrip.getHeadsignId());
				return true;
			}
		} else if (rsn == 12L) {
			if (Arrays.asList( //
					"North Vly Connector", // <>
					"Oyster River" //
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString("Oyster River", mTrip.getHeadsignId());
				return true;
			}
			if (Arrays.asList( //
					"North Vly Connector", // <>
					"Downtown Courtenay" //
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString("Downtown Courtenay", mTrip.getHeadsignId());
				return true;
			}
		}
		throw new MTLog.Fatal("%s: %d: Unexpected trips to merge: %s & %s!", mTrip.getRouteId(), rsn, mTrip, mTripToMerge);
	}

	private static final String EXCH = "Exch";
	private static final Pattern EXCHANGE = Pattern.compile("((^|\\W)(exchange)(\\W|$))", Pattern.CASE_INSENSITIVE);
	private static final String EXCHANGE_REPLACEMENT = "$2" + EXCH + "$4";

	private static final Pattern STARTS_WITH_NUMBER = Pattern.compile("(^[\\d]+[\\S]*)", Pattern.CASE_INSENSITIVE);

	private static final Pattern DOWNTOWN_ = Pattern.compile("((^|\\W)(downtwon)(\\W|$))", Pattern.CASE_INSENSITIVE);
	private static final String DOWNTOWN_REPLACEMENT = "$2" + "Downtown" + "$4";

	@NotNull
	@Override
	public String cleanTripHeadsign(@NotNull String tripHeadsign) {
		tripHeadsign = EXCHANGE.matcher(tripHeadsign).replaceAll(EXCHANGE_REPLACEMENT);
		tripHeadsign = DOWNTOWN_.matcher(tripHeadsign).replaceAll(DOWNTOWN_REPLACEMENT);
		tripHeadsign = CleanUtils.toLowerCaseUpperCaseWords(Locale.ENGLISH, tripHeadsign, "VMP", "FS", "NIC");
		tripHeadsign = CleanUtils.keepToAndRemoveVia(tripHeadsign);
		tripHeadsign = CleanUtils.CLEAN_AND.matcher(tripHeadsign).replaceAll(CleanUtils.CLEAN_AND_REPLACEMENT);
		tripHeadsign = STARTS_WITH_NUMBER.matcher(tripHeadsign).replaceAll(StringUtils.EMPTY);
		tripHeadsign = CleanUtils.cleanStreetTypes(tripHeadsign);
		tripHeadsign = CleanUtils.cleanNumbers(tripHeadsign);
		return CleanUtils.cleanLabel(tripHeadsign);
	}

	private static final Pattern STARTS_WITH_DCOM = Pattern.compile("(^(\\(-DCOM-\\)))", Pattern.CASE_INSENSITIVE);
	private static final Pattern STARTS_WITH_IMPL = Pattern.compile("(^(\\(-IMPL-\\)))", Pattern.CASE_INSENSITIVE);

	@NotNull
	@Override
	public String cleanStopName(@NotNull String gStopName) {
		gStopName = STARTS_WITH_DCOM.matcher(gStopName).replaceAll(EMPTY);
		gStopName = STARTS_WITH_IMPL.matcher(gStopName).replaceAll(EMPTY);
		gStopName = EXCHANGE.matcher(gStopName).replaceAll(EXCHANGE_REPLACEMENT);
		gStopName = CleanUtils.toLowerCaseUpperCaseWords(Locale.ENGLISH, gStopName, "VMP", "FS", "NIC");
		gStopName = CleanUtils.cleanBounds(gStopName);
		gStopName = CleanUtils.CLEAN_AT.matcher(gStopName).replaceAll(CleanUtils.CLEAN_AT_REPLACEMENT);
		gStopName = CleanUtils.cleanStreetTypes(gStopName);
		gStopName = CleanUtils.cleanNumbers(gStopName);
		return CleanUtils.cleanLabel(gStopName);
	}

	@Override
	public int getStopId(@NotNull GStop gStop) { // used by GTFS-RT
		return super.getStopId(gStop);
	}
}
