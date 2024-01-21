package org.mtransit.parser.ca_comox_valley_transit_system_bus;

import static org.mtransit.commons.Constants.EMPTY;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.mtransit.commons.CleanUtils;
import org.mtransit.commons.StringUtils;
import org.mtransit.parser.DefaultAgencyTools;
import org.mtransit.parser.MTLog;
import org.mtransit.parser.gtfs.data.GRoute;
import org.mtransit.parser.gtfs.data.GStop;
import org.mtransit.parser.mt.data.MAgency;
import org.mtransit.parser.mt.data.MRoute;

import java.util.Locale;
import java.util.regex.Pattern;

// https://www.bctransit.com/open-data
public class ComoxValleyTransitSystemBusAgencyTools extends DefaultAgencyTools {

	public static void main(@NotNull String[] args) {
		new ComoxValleyTransitSystemBusAgencyTools().start(args);
	}

	@Override
	public boolean defaultExcludeEnabled() {
		return true;
	}

	@NotNull
	@Override
	public String getAgencyName() {
		return "Comox Valley TS";
	}

	@NotNull
	@Override
	public Integer getAgencyRouteType() {
		return MAgency.ROUTE_TYPE_BUS;
	}

	@Override
	public boolean defaultRouteIdEnabled() {
		return true;
	}

	@Override
	public boolean useRouteShortNameForRouteId() {
		return false; // route ID used by GTFS-RT
	}

	@Override
	public @Nullable String getRouteIdCleanupRegex() {
		return "\\-[A-Z]+$";
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
	public String cleanRouteLongName(@NotNull String routeLongName) {
		routeLongName = CleanUtils.cleanSlashes(routeLongName);
		routeLongName = CleanUtils.cleanNumbers(routeLongName);
		routeLongName = CleanUtils.cleanStreetTypes(routeLongName);
		return CleanUtils.cleanLabel(routeLongName);
	}

	@Nullable
	@Override
	public String fixColor(@Nullable String color) {
		if ("000000".equals(color)) {
			return null;
		}
		return super.fixColor(color);
	}

	@Override
	public boolean defaultAgencyColorEnabled() {
		return true;
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
	public String provideMissingRouteColor(@NotNull GRoute gRoute) {
		final int rsn = Integer.parseInt(gRoute.getRouteShortName());
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

	@Override
	public boolean directionFinderEnabled() {
		return true;
	}

	private static final Pattern AIRPORT_VIA_POWELL_R_FERRY = Pattern.compile("(airport - powell r\\.? ferry)", Pattern.CASE_INSENSITIVE);
	private static final String AIRPORT_VIA_POWELL_R_FERRY_REPLACEMENT = "Airport";

	@NotNull
	@Override
	public String cleanDirectionHeadsign(int directionId, boolean fromStopName, @NotNull String directionHeadSign) {
		directionHeadSign = super.cleanDirectionHeadsign(directionId, fromStopName, directionHeadSign);
		directionHeadSign = AIRPORT_VIA_POWELL_R_FERRY.matcher(directionHeadSign).replaceAll(AIRPORT_VIA_POWELL_R_FERRY_REPLACEMENT);
		return directionHeadSign;
	}

	private static final Pattern STARTS_WITH_NUMBER = Pattern.compile("(^\\d+\\S*)", Pattern.CASE_INSENSITIVE);

	private static final Pattern FIX_DOWNTOWN_ = CleanUtils.cleanWord("downtwon");
	private static final String FIX_DOWNTOWN_REPLACEMENT = CleanUtils.cleanWordsReplacement("Downtown");

	private static final Pattern BAY_AZ_ = CleanUtils.cleanWords("bay [a-z]");

	@NotNull
	@Override
	public String cleanTripHeadsign(@NotNull String tripHeadsign) {
		tripHeadsign = FIX_DOWNTOWN_.matcher(tripHeadsign).replaceAll(FIX_DOWNTOWN_REPLACEMENT);
		tripHeadsign = BAY_AZ_.matcher(tripHeadsign).replaceAll(EMPTY);
		tripHeadsign = CleanUtils.toLowerCaseUpperCaseWords(Locale.ENGLISH, tripHeadsign, getIgnoredWords());
		tripHeadsign = CleanUtils.keepToAndRemoveVia(tripHeadsign);
		tripHeadsign = CleanUtils.CLEAN_AND.matcher(tripHeadsign).replaceAll(CleanUtils.CLEAN_AND_REPLACEMENT);
		tripHeadsign = STARTS_WITH_NUMBER.matcher(tripHeadsign).replaceAll(StringUtils.EMPTY);
		tripHeadsign = CleanUtils.cleanStreetTypes(tripHeadsign);
		tripHeadsign = CleanUtils.cleanNumbers(tripHeadsign);
		return CleanUtils.cleanLabel(tripHeadsign);
	}

	@NotNull
	private String[] getIgnoredWords() {
		return new String[]{"VMP", "FS", "NIC", "AM", "PM"};
	}

	private static final Pattern STARTS_WITH_DCOM = Pattern.compile("(^(\\(-DCOM-\\)))", Pattern.CASE_INSENSITIVE);
	private static final Pattern STARTS_WITH_IMPL = Pattern.compile("(^(\\(-IMPL-\\)))", Pattern.CASE_INSENSITIVE);

	@NotNull
	@Override
	public String cleanStopName(@NotNull String gStopName) {
		gStopName = STARTS_WITH_DCOM.matcher(gStopName).replaceAll(EMPTY);
		gStopName = STARTS_WITH_IMPL.matcher(gStopName).replaceAll(EMPTY);
		gStopName = CleanUtils.toLowerCaseUpperCaseWords(Locale.ENGLISH, gStopName, getIgnoredWords());
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
