package games.strategy.triplea.attachments;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import games.strategy.engine.data.Attachable;
import games.strategy.engine.data.DefaultAttachment;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GameParseException;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Resource;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.engine.data.annotations.GameProperty;
import games.strategy.triplea.Constants;
import games.strategy.triplea.MapSupport;
import games.strategy.triplea.Properties;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.delegate.TechTracker;
import games.strategy.triplea.formatter.MyFormatter;
import games.strategy.util.IntegerMap;
import games.strategy.util.Match;
import games.strategy.util.Tuple;
import games.strategy.util.Util;

/**
 * Despite the misleading name, this attaches not to individual Units but to UnitTypes.
 * Please follow this naming convention:
 * if the property is called "m_fooBar"
 * then you must have a "setFooBar" and "getFooBar",
 * and if the set method adds to a list or map, then you also need a "clearFooBar".
 * Do not change the name fooBar to make it plural or any other crap.
 */
@MapSupport
public class UnitAttachment extends DefaultAttachment {
  private static final long serialVersionUID = -2946748686268541820L;

  /**
   * Convenience method.
   */
  public static UnitAttachment get(final UnitType type) {
    return get(type, Constants.UNIT_ATTACHMENT_NAME);
  }

  static UnitAttachment get(final UnitType type, final String nameOfAttachment) {
    return getAttachment(type, nameOfAttachment, UnitAttachment.class);
  }

  private static Collection<UnitType> getUnitTypesFromUnitList(final Collection<Unit> units) {
    final Collection<UnitType> types = new ArrayList<>();
    for (final Unit u : units) {
      if (!types.contains(u.getType())) {
        types.add(u.getType());
      }
    }
    return types;
  }

  public static final String UNITSMAYNOTLANDONCARRIER = "unitsMayNotLandOnCarrier";
  public static final String UNITSMAYNOTLEAVEALLIEDCARRIER = "unitsMayNotLeaveAlliedCarrier";
  // movement related
  private boolean m_isAir = false;
  private boolean m_isSea = false;
  private int m_movement = 0;
  private boolean m_canBlitz = false;
  private boolean m_isKamikaze = false;
  // a colon delimited list of transports where this unit may invade from, it supports "none"
  // and if empty it allows you to invade from all
  private String[] m_canInvadeOnlyFrom = null;
  private IntegerMap<Resource> m_fuelCost = new IntegerMap<>();
  private boolean m_canNotMoveDuringCombatMove = false;
  private Tuple<Integer, String> m_movementLimit = null;
  // combat related
  private int m_attack = 0;
  private int m_defense = 0;
  private boolean m_isInfrastructure = false;
  private boolean m_canBombard = false;
  private int m_bombard = -1;
  private boolean m_isSub = false;
  private boolean m_isDestroyer = false;
  private boolean m_artillery = false;
  private boolean m_artillerySupportable = false;
  private int m_unitSupportCount = -1;
  private int m_isMarine = 0;
  private boolean m_isSuicide = false;
  private Tuple<Integer, String> m_attackingLimit = null;
  private int m_attackRolls = 1;
  private int m_defenseRolls = 1;
  private boolean m_chooseBestRoll = false;
  // transportation related
  private boolean m_isCombatTransport = false;
  // -1 if cant transport
  private int m_transportCapacity = -1;
  // -1 if cant be transported
  private int m_transportCost = -1;
  // -1 if cant act as a carrier
  private int m_carrierCapacity = -1;
  // -1 if cant land on a carrier
  private int m_carrierCost = -1;
  private boolean m_isAirTransport = false;
  private boolean m_isAirTransportable = false;
  private boolean m_isInfantry = false;
  private boolean m_isLandTransport = false;
  // aa related
  // "isAA" and "isAAmovement" are also valid setters, used as shortcuts for calling multiple aa related setters. Must
  // keep.
  private boolean m_isAAforCombatOnly = false;
  private boolean m_isAAforBombingThisUnitOnly = false;
  private boolean m_isAAforFlyOverOnly = false;
  private boolean m_isRocket = false;
  private int m_attackAA = 1;
  private int m_offensiveAttackAA = 0;
  private int m_attackAAmaxDieSides = -1;
  private int m_offensiveAttackAAmaxDieSides = -1;
  // -1 means infinite
  private int m_maxAAattacks = -1;
  // -1 means infinite
  private int m_maxRoundsAA = 1;
  // default value for when it is not set
  private String m_typeAA = "AA";
  // null means targeting air units only
  private HashSet<UnitType> m_targetsAA = null;
  // if false, we cannot shoot more times than there are number of planes
  private boolean m_mayOverStackAA = false;
  // if false, we instantly kill anything our AA shot hits
  private boolean m_damageableAA = false;
  // if these enemy units are present, the gun does not fire at all
  private HashSet<UnitType> m_willNotFireIfPresent = new HashSet<>();
  // strategic bombing related
  private boolean m_isStrategicBomber = false;
  private int m_bombingMaxDieSides = -1;
  private int m_bombingBonus = -1;
  private boolean m_canIntercept = false;
  private boolean m_canEscort = false;
  private boolean m_canAirBattle = false;
  private int m_airDefense = 0;
  private int m_airAttack = 0;
  // null means they can target any unit that can be damaged
  private HashSet<UnitType> m_bombingTargets = null;
  // production related
  // this has been split into canProduceUnits, isConstruction, canBeDamaged, and isInfrastructure
  // private boolean m_isFactory = false;
  private boolean m_canProduceUnits = false;
  // -1 means either it can't produce any, or it produces at the value of the territory it is located in
  private int m_canProduceXUnits = -1;
  private IntegerMap<UnitType> m_createsUnitsList = new IntegerMap<>();
  private IntegerMap<Resource> m_createsResourcesList = new IntegerMap<>();
  // damage related
  private int m_hitPoints = 1;
  private boolean m_canBeDamaged = false;
  // this is bombing damage, not hitpoints. default of 2 means that factories will take 2x the territory value
  // they are in, of damage.
  private int m_maxDamage = 2;
  // -1 if can't be disabled
  private int m_maxOperationalDamage = -1;
  private boolean m_canDieFromReachingMaxDamage = false;
  // placement related
  private boolean m_isConstruction = false;
  // can be any String except for "none" if isConstruction is true
  private String m_constructionType = "none";
  // -1 if not set, is meaningless
  private int m_constructionsPerTerrPerTypePerTurn = -1;
  // -1 if not set, is meaningless
  private int m_maxConstructionsPerTypePerTerr = -1;
  // -1 means anywhere
  private int m_canOnlyBePlacedInTerritoryValuedAtX = -1;
  // multiple colon delimited lists of the unit combos required for
  // this unit to be built somewhere. (units must be in same
  // territory, owned by player, not be disabled)
  private ArrayList<String[]> m_requiresUnits = new ArrayList<>();
  private IntegerMap<UnitType> m_consumesUnits = new IntegerMap<>();
  // multiple colon delimited lists of the unit combos required for
  // this unit to move into a territory. (units must be owned by player, not be disabled)
  private ArrayList<String[]> m_requiresUnitsToMove = new ArrayList<>();
  // a colon delimited list of territories where this unit may not be placed
  // also an allowed setter is "setUnitPlacementOnlyAllowedIn",
  // which just creates m_unitPlacementRestrictions with an inverted list of territories
  private String[] m_unitPlacementRestrictions = null;
  // -1 if infinite (infinite is default)
  private int m_maxBuiltPerPlayer = -1;
  private Tuple<Integer, String> m_placementLimit = null;
  // scrambling related
  private boolean m_canScramble = false;
  private boolean m_isAirBase = false;
  // -1 if can't scramble
  private int m_maxScrambleDistance = -1;
  // -1 for infinite
  private int m_maxScrambleCount = -1;
  // special abilities
  private int m_blockade = 0;
  // a colon delimited list of the units this unit can repair.
  // (units must be in same territory, unless this unit is land
  // and the repaired unit is sea)
  private IntegerMap<UnitType> m_repairsUnits = new IntegerMap<>();
  private IntegerMap<UnitType> m_givesMovement = new IntegerMap<>();
  private ArrayList<Tuple<String, PlayerID>> m_destroyedWhenCapturedBy = new ArrayList<>();
  // also an allowed setter is "setDestroyedWhenCapturedFrom" which will just create m_destroyedWhenCapturedBy with a
  // specific list
  private LinkedHashMap<String, Tuple<String, IntegerMap<UnitType>>> m_whenCapturedChangesInto =
      new LinkedHashMap<>();
  private ArrayList<PlayerID> m_canBeCapturedOnEnteringBy = new ArrayList<>();
  private ArrayList<PlayerID> m_canBeGivenByTerritoryTo = new ArrayList<>();
  // a set of information for dealing with special abilities or
  // loss of abilities when a unit takes x-y amount of damage
  private ArrayList<Tuple<Tuple<Integer, Integer>, Tuple<String, String>>> m_whenCombatDamaged =
      new ArrayList<>();
  // a kind of support attachment for giving actual unit
  // attachment abilities or other to a unit, when in the
  // precense or on the same route with another unit
  private ArrayList<String> m_receivesAbilityWhenWith = new ArrayList<>();
  // currently used for: placement in original territories only
  private HashSet<String> m_special = new HashSet<>();
  // Manually set TUV
  private int m_tuv = -1;

  /** Creates new UnitAttachment. */
  public UnitAttachment(final String name, final Attachable attachable, final GameData gameData) {
    super(name, attachable, gameData);
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setCanIntercept(final String value) {
    m_canIntercept = getBool(value);
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setCanIntercept(final boolean value) {
    m_canIntercept = value;
  }

  public boolean getCanIntercept() {
    return m_canIntercept;
  }

  public void resetCanIntercept() {
    m_canIntercept = false;
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setCanEscort(final String value) {
    m_canEscort = getBool(value);
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setCanEscort(final boolean value) {
    m_canEscort = value;
  }

  public boolean getCanEscort() {
    return m_canEscort;
  }

  public void resetCanEscort() {
    m_canEscort = false;
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setCanAirBattle(final String value) {
    m_canAirBattle = getBool(value);
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setCanAirBattle(final Boolean value) {
    m_canAirBattle = value;
  }

  public boolean getCanAirBattle() {
    return m_canAirBattle;
  }

  public void resetCanAirBattle() {
    m_canAirBattle = false;
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setAirDefense(final String value) {
    m_airDefense = getInt(value);
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setAirDefense(final Integer value) {
    m_airDefense = value;
  }

  public int getAirDefense() {
    return m_airDefense;
  }

  public int getAirDefense(final PlayerID player) {
    return (Math.min(getData().getDiceSides(), Math.max(0,
        m_airDefense + TechAbilityAttachment.getAirDefenseBonus((UnitType) this.getAttachedTo(), player, getData()))));
  }

  public void resetAirDefense() {
    m_airDefense = 0;
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setAirAttack(final String value) {
    m_airAttack = getInt(value);
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setAirAttack(final Integer value) {
    m_airAttack = value;
  }

  public int getAirAttack() {
    return m_airAttack;
  }

  public int getAirAttack(final PlayerID player) {
    return (Math.min(getData().getDiceSides(), Math.max(0,
        m_airAttack + TechAbilityAttachment.getAirAttackBonus((UnitType) this.getAttachedTo(), player, getData()))));
  }

  public void resetAirAttack() {
    m_airAttack = 0;
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setIsAirTransport(final String s) {
    m_isAirTransport = getBool(s);
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setIsAirTransport(final Boolean s) {
    m_isAirTransport = s;
  }

  public boolean getIsAirTransport() {
    return m_isAirTransport;
  }

  public void resetIsAirTransport() {
    m_isAirTransport = false;
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setIsAirTransportable(final String s) {
    m_isAirTransportable = getBool(s);
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setIsAirTransportable(final Boolean s) {
    m_isAirTransportable = s;
  }

  public boolean getIsAirTransportable() {
    return m_isAirTransportable;
  }

  public void resetIsAirTransportable() {
    m_isAirTransportable = false;
  }

  /**
   * Adds to, not sets. Anything that adds to instead of setting needs a clear function as well.
   */
  @GameProperty(xmlProperty = true, gameProperty = true, adds = true)
  public void setCanBeGivenByTerritoryTo(final String value) throws GameParseException {
    final String[] temp = value.split(":");
    for (final String name : temp) {
      final PlayerID tempPlayer = getData().getPlayerList().getPlayerId(name);
      if (tempPlayer != null) {
        m_canBeGivenByTerritoryTo.add(tempPlayer);
      } else if (name.equalsIgnoreCase("true") || name.equalsIgnoreCase("false")) {
        m_canBeGivenByTerritoryTo.clear();
      } else {
        throw new GameParseException("No player named: " + name + thisErrorMsg());
      }
    }
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setCanBeGivenByTerritoryTo(final ArrayList<PlayerID> value) {
    m_canBeGivenByTerritoryTo = value;
  }

  public ArrayList<PlayerID> getCanBeGivenByTerritoryTo() {
    return m_canBeGivenByTerritoryTo;
  }

  public void clearCanBeGivenByTerritoryTo() {
    m_canBeGivenByTerritoryTo.clear();
  }

  public void resetCanBeGivenByTerritoryTo() {
    m_canBeGivenByTerritoryTo = new ArrayList<>();
  }

  /**
   * Adds to, not sets. Anything that adds to instead of setting needs a clear function as well.
   */
  @GameProperty(xmlProperty = true, gameProperty = true, adds = true)
  public void setCanBeCapturedOnEnteringBy(final String value) throws GameParseException {
    final String[] temp = value.split(":");
    for (final String name : temp) {
      final PlayerID tempPlayer = getData().getPlayerList().getPlayerId(name);
      if (tempPlayer != null) {
        m_canBeCapturedOnEnteringBy.add(tempPlayer);
      } else {
        throw new GameParseException("No player named: " + name + thisErrorMsg());
      }
    }
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setCanBeCapturedOnEnteringBy(final ArrayList<PlayerID> value) {
    m_canBeCapturedOnEnteringBy = value;
  }

  public ArrayList<PlayerID> getCanBeCapturedOnEnteringBy() {
    return m_canBeCapturedOnEnteringBy;
  }

  public void clearCanBeCapturedOnEnteringBy() {
    m_canBeCapturedOnEnteringBy.clear();
  }

  public void resetCanBeCapturedOnEnteringBy() {
    m_canBeCapturedOnEnteringBy = new ArrayList<>();
  }

  /**
   * Adds to, not sets. Anything that adds to instead of setting needs a clear function as well.
   */
  @GameProperty(xmlProperty = true, gameProperty = true, adds = true)
  public void setWhenCapturedChangesInto(final String value) throws GameParseException {
    final String[] s = value.split(":");
    if (s.length < 5 || (s.length - 1) % 2 != 0) {
      throw new GameParseException("whenCapturedChangesInto must have 5 or more values, "
          + "playerFrom:playerTo:keepAttributes:unitType:howMany "
          + "(you may have additional unitType:howMany:unitType:howMany, etc" + thisErrorMsg());
    }
    final PlayerID pfrom = getData().getPlayerList().getPlayerId(s[0]);
    if (pfrom == null && !s[0].equals("any")) {
      throw new GameParseException("whenCapturedChangesInto: No player named: " + s[0] + thisErrorMsg());
    }
    final PlayerID pto = getData().getPlayerList().getPlayerId(s[1]);
    if (pto == null && !s[1].equals("any")) {
      throw new GameParseException("whenCapturedChangesInto: No player named: " + s[1] + thisErrorMsg());
    }
    getBool(s[2]);
    final IntegerMap<UnitType> unitsToMake = new IntegerMap<>();
    for (int i = 3; i < s.length; i++) {
      final UnitType ut = getData().getUnitTypeList().getUnitType(s[i]);
      if (ut == null) {
        throw new GameParseException("whenCapturedChangesInto: No unit named: " + s[3] + thisErrorMsg());
      }
      i++;
      final int howMany = getInt(s[i]);
      unitsToMake.put(ut, howMany);
    }
    m_whenCapturedChangesInto.put(s[0] + ":" + s[1], Tuple.of(s[2], unitsToMake));
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setWhenCapturedChangesInto(final LinkedHashMap<String, Tuple<String, IntegerMap<UnitType>>> value) {
    m_whenCapturedChangesInto = value;
  }

  public LinkedHashMap<String, Tuple<String, IntegerMap<UnitType>>> getWhenCapturedChangesInto() {
    return m_whenCapturedChangesInto;
  }

  public void clearWhenCapturedChangesInto() {
    m_whenCapturedChangesInto.clear();
  }

  public void resetWhenCapturedChangesInto() {
    m_whenCapturedChangesInto = new LinkedHashMap<>();
  }

  /**
   * Adds to, not sets. Anything that adds to instead of setting needs a clear function as well.
   */
  @GameProperty(xmlProperty = true, gameProperty = true, adds = true)
  public void setDestroyedWhenCapturedBy(String value) throws GameParseException {
    // We can prefix this value with "BY" or "FROM" to change the setting. If no setting, default to "BY" since this
    // this is called by
    // destroyedWhenCapturedBy
    String byOrFrom = "BY";
    if (value.startsWith("BY:") && getData().getPlayerList().getPlayerId("BY") == null) {
      byOrFrom = "BY";
      value = value.replaceFirst("BY:", "");
    } else if (value.startsWith("FROM:") && getData().getPlayerList().getPlayerId("FROM") == null) {
      byOrFrom = "FROM";
      value = value.replaceFirst("FROM:", "");
    }
    final String[] temp = value.split(":");
    for (final String name : temp) {
      final PlayerID tempPlayer = getData().getPlayerList().getPlayerId(name);
      if (tempPlayer != null) {
        m_destroyedWhenCapturedBy.add(Tuple.of(byOrFrom, tempPlayer));
      } else {
        throw new GameParseException("No player named: " + name + thisErrorMsg());
      }
    }
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setDestroyedWhenCapturedBy(final ArrayList<Tuple<String, PlayerID>> value) {
    m_destroyedWhenCapturedBy = value;
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = true, virtual = true)
  public void setDestroyedWhenCapturedFrom(String value) throws GameParseException {
    if (!(value.startsWith("BY:") || value.startsWith("FROM:"))) {
      value = "FROM:" + value;
    }
    setDestroyedWhenCapturedBy(value);
  }

  public ArrayList<Tuple<String, PlayerID>> getDestroyedWhenCapturedBy() {
    return m_destroyedWhenCapturedBy;
  }

  public void clearDestroyedWhenCapturedBy() {
    m_destroyedWhenCapturedBy.clear();
  }

  public void resetDestroyedWhenCapturedBy() {
    m_destroyedWhenCapturedBy = new ArrayList<>();
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setCanBlitz(final String s) {
    m_canBlitz = getBool(s);
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setCanBlitz(final Boolean s) {
    m_canBlitz = s;
  }

  public boolean getCanBlitz() {
    return m_canBlitz;
  }

  public boolean getCanBlitz(final PlayerID player) {
    if (m_canBlitz) {
      return true;
    }
    return TechAbilityAttachment.getUnitAbilitiesGained(TechAbilityAttachment.ABILITY_CAN_BLITZ,
        (UnitType) this.getAttachedTo(), player, getData());
  }

  public void resetCanBlitz() {
    m_canBlitz = false;
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setIsSub(final String s) {
    m_isSub = getBool(s);
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setIsSub(final Boolean s) {
    m_isSub = s;
  }

  public boolean getIsSub() {
    return m_isSub;
  }

  public void resetIsSub() {
    m_isSub = false;
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setIsCombatTransport(final String s) {
    m_isCombatTransport = getBool(s);
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setIsCombatTransport(final Boolean s) {
    m_isCombatTransport = s;
  }

  public boolean getIsCombatTransport() {
    return m_isCombatTransport;
  }

  public void resetIsCombatTransport() {
    m_isCombatTransport = false;
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setIsStrategicBomber(final String s) {
    m_isStrategicBomber = getBool(s);
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setIsStrategicBomber(final Boolean s) {
    m_isStrategicBomber = s;
  }

  public boolean getIsStrategicBomber() {
    return m_isStrategicBomber;
  }

  public void resetIsStrategicBomber() {
    m_isStrategicBomber = false;
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setIsDestroyer(final String s) {
    m_isDestroyer = getBool(s);
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setIsDestroyer(final Boolean s) {
    m_isDestroyer = s;
  }

  public boolean getIsDestroyer() {
    return m_isDestroyer;
  }

  public void resetIsDestroyer() {
    m_isDestroyer = false;
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setCanBombard(final String s) {
    m_canBombard = getBool(s);
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setCanBombard(final Boolean s) {
    m_canBombard = s;
  }

  public boolean getCanBombard() {
    return m_canBombard;
  }

  public boolean getCanBombard(final PlayerID player) {
    if (m_canBombard) {
      return true;
    }
    return TechAbilityAttachment.getUnitAbilitiesGained(TechAbilityAttachment.ABILITY_CAN_BOMBARD,
        (UnitType) this.getAttachedTo(), player, getData());
  }

  public void resetCanBombard() {
    m_canBombard = false;
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setIsAir(final String s) {
    m_isAir = getBool(s);
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setIsAir(final Boolean s) {
    m_isAir = s;
  }

  public boolean getIsAir() {
    return m_isAir;
  }

  public void resetIsAir() {
    m_isAir = false;
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setIsSea(final String s) {
    m_isSea = getBool(s);
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setIsSea(final Boolean s) {
    m_isSea = s;
  }

  public boolean getIsSea() {
    return m_isSea;
  }

  public void resetIsSea() {
    m_isSea = false;
  }

  // DO NOT REMOVE, this is an important convenience method for xmls
  @GameProperty(xmlProperty = true, gameProperty = true, adds = false, virtual = true)
  public void setIsFactory(final String s) {
    setIsFactory(getBool(s));
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false, virtual = true)
  public void setIsFactory(final Boolean s) {
    setCanBeDamaged(s);
    setIsInfrastructure(s);
    setCanProduceUnits(s);
    setIsConstruction(s);
    if (s) {
      setConstructionType(Constants.CONSTRUCTION_TYPE_FACTORY);
      setMaxConstructionsPerTypePerTerr("1");
      setConstructionsPerTerrPerTypePerTurn("1");
    } else {
      // return to defaults
      setConstructionType("none");
      setMaxConstructionsPerTypePerTerr("-1");
      setConstructionsPerTerrPerTypePerTurn("-1");
    }
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setCanProduceUnits(final String s) {
    m_canProduceUnits = getBool(s);
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setCanProduceUnits(final Boolean s) {
    m_canProduceUnits = s;
  }

  public boolean getCanProduceUnits() {
    return m_canProduceUnits;
  }

  public void resetCanProduceUnits() {
    m_canProduceUnits = false;
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setCanProduceXUnits(final String s) {
    m_canProduceXUnits = getInt(s);
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setCanProduceXUnits(final Integer s) {
    m_canProduceXUnits = s;
  }

  public int getCanProduceXUnits() {
    return m_canProduceXUnits;
  }

  public void resetCanProduceXUnits() {
    m_canProduceXUnits = -1;
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setCanOnlyBePlacedInTerritoryValuedAtX(final String s) {
    m_canOnlyBePlacedInTerritoryValuedAtX = getInt(s);
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setCanOnlyBePlacedInTerritoryValuedAtX(final Integer s) {
    m_canOnlyBePlacedInTerritoryValuedAtX = s;
  }

  public int getCanOnlyBePlacedInTerritoryValuedAtX() {
    return m_canOnlyBePlacedInTerritoryValuedAtX;
  }

  public void resetCanOnlyBePlacedInTerritoryValuedAtX() {
    m_canOnlyBePlacedInTerritoryValuedAtX = -1;
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setUnitPlacementRestrictions(final String value) {
    if (value == null) {
      m_unitPlacementRestrictions = null;
      return;
    }
    m_unitPlacementRestrictions = value.split(":");
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setUnitPlacementRestrictions(final String[] value) {
    m_unitPlacementRestrictions = value;
  }

  public String[] getUnitPlacementRestrictions() {
    return m_unitPlacementRestrictions;
  }

  public void resetUnitPlacementRestrictions() {
    m_unitPlacementRestrictions = null;
  }

  // no m_ variable for this, since it is the inverse of m_unitPlacementRestrictions
  // we might as well just use m_unitPlacementRestrictions
  @GameProperty(xmlProperty = true, gameProperty = true, adds = false, virtual = true)
  public void setUnitPlacementOnlyAllowedIn(final String value) throws GameParseException {
    final Collection<Territory> allowedTerritories = getListedTerritories(value.split(":"));
    final Collection<Territory> restrictedTerritories = new HashSet<>(getData().getMap().getTerritories());
    restrictedTerritories.removeAll(allowedTerritories);
    m_unitPlacementRestrictions = restrictedTerritories.stream()
        .map(Territory::getName)
        .toArray(size -> new String[size]);
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = true)
  public void setRepairsUnits(final String value) throws GameParseException {
    final String[] s = value.split(":");
    if (s.length <= 0) {
      throw new GameParseException("repairsUnits cannot be empty" + thisErrorMsg());
    }
    int i = 0;
    int amount;
    try {
      amount = Integer.parseInt(s[0]);
      i++;
    } catch (final NumberFormatException nfe) {
      amount = 1;
    }
    for (; i < s.length; i++) {
      final UnitType ut = getData().getUnitTypeList().getUnitType(s[i]);
      if (ut == null) {
        throw new GameParseException("No unit called:" + s[i] + thisErrorMsg());
      }
      m_repairsUnits.put(ut, amount);
    }
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setRepairsUnits(final IntegerMap<UnitType> value) {
    m_repairsUnits = value;
  }

  public IntegerMap<UnitType> getRepairsUnits() {
    return m_repairsUnits;
  }

  public void clearRepairsUnits() {
    m_repairsUnits.clear();
  }

  public void resetRepairsUnits() {
    m_repairsUnits = new IntegerMap<>();
  }

  /**
   * Adds to, not sets. Anything that adds to instead of setting needs a clear function as well.
   */
  @GameProperty(xmlProperty = true, gameProperty = true, adds = true)
  public void setSpecial(final String value) throws GameParseException {
    final String[] s = value.split(":");
    for (final String option : s) {
      if (!(option.equals("none") || option.equals("canOnlyPlaceInOriginalTerritories"))) {
        throw new GameParseException("special does not allow: " + option + thisErrorMsg());
      }
      m_special.add(option);
    }
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setSpecial(final HashSet<String> value) {
    m_special = value;
  }

  public HashSet<String> getSpecial() {
    return m_special;
  }

  public void clearSpecial() {
    m_special.clear();
  }

  public void resetSpecial() {
    m_special = new HashSet<>();
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setCanInvadeOnlyFrom(final String value) {
    if (value == null) {
      m_canInvadeOnlyFrom = null;
      return;
    }
    final String[] canOnlyInvadeFrom = value.split(":");
    if (canOnlyInvadeFrom[0].toLowerCase().equals("none")) {
      m_canInvadeOnlyFrom = new String[] {"none"};
      return;
    }
    if (canOnlyInvadeFrom[0].toLowerCase().equals("all")) {
      m_canInvadeOnlyFrom = new String[] {"all"};
      return;
    }
    m_canInvadeOnlyFrom = canOnlyInvadeFrom;
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setCanInvadeOnlyFrom(final String[] value) {
    m_canInvadeOnlyFrom = value;
  }

  public String[] getCanInvadeOnlyFrom() {
    return m_canInvadeOnlyFrom;
  }

  public boolean canInvadeFrom(final String transport) {
    final UnitType ut = getData().getUnitTypeList().getUnitType(transport);
    if (ut == null) {
      throw new IllegalStateException("No unit called:" + transport + thisErrorMsg());
    }
    // (UnitAttachment) ut.getAttachments().values().iterator().next();
    // UnitAttachment ua = UnitAttachment.get(ut);
    // Units may be considered transported if they are on a carrier, or if they are paratroopers, or if they are mech
    // infantry. The
    // "transporter" may not be an actual transport, so we should not check for that here.
    if (m_canInvadeOnlyFrom == null || Arrays.asList(m_canInvadeOnlyFrom).isEmpty() || m_canInvadeOnlyFrom[0].equals("")
        || m_canInvadeOnlyFrom[0].equals("all")) {
      return true;
    }
    return Arrays.asList(m_canInvadeOnlyFrom).contains(transport);
  }

  public void resetCanInvadeOnlyFrom() {
    m_canInvadeOnlyFrom = null;
  }

  /**
   * Adds to, not sets. Anything that adds to instead of setting needs a clear function as well.
   */
  @GameProperty(xmlProperty = true, gameProperty = true, adds = true)
  public void setRequiresUnits(final String value) {
    m_requiresUnits.add(value.split(":"));
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setRequiresUnits(final ArrayList<String[]> value) {
    m_requiresUnits = value;
  }

  public ArrayList<String[]> getRequiresUnits() {
    return m_requiresUnits;
  }

  public void clearRequiresUnits() {
    m_requiresUnits.clear();
  }

  public void resetRequiresUnits() {
    m_requiresUnits = new ArrayList<>();
  }

  /**
   * Adds to, not sets. Anything that adds to instead of setting needs a clear function as well.
   */
  @GameProperty(xmlProperty = true, gameProperty = true, adds = true)
  public void setRequiresUnitsToMove(final String value) throws GameParseException {
    final String[] array = value.split(":");
    if (array.length == 0) {
      throw new GameParseException("requiresUnitsToMove must have at least 1 unit type" + thisErrorMsg());
    }
    for (final String s : array) {
      final UnitType ut = getData().getUnitTypeList().getUnitType(s);
      if (ut == null) {
        throw new GameParseException("No unit called:" + s + thisErrorMsg());
      }
    }
    m_requiresUnitsToMove.add(array);
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setRequiresUnitsToMove(final ArrayList<String[]> value) {
    m_requiresUnitsToMove = value;
  }

  public ArrayList<String[]> getRequiresUnitsToMove() {
    return m_requiresUnitsToMove;
  }

  public void clearRequiresUnitsToMove() {
    m_requiresUnitsToMove.clear();
  }

  public void resetRequiresUnitsToMove() {
    m_requiresUnitsToMove = new ArrayList<>();
  }

  /**
   * Adds to, not sets. Anything that adds to instead of setting needs a clear function as well.
   */
  @GameProperty(xmlProperty = true, gameProperty = true, adds = true)
  public void setWhenCombatDamaged(final String value) throws GameParseException {
    final String[] s = value.split(":");
    if (!(s.length == 3 || s.length == 4)) {
      throw new GameParseException(
          "whenCombatDamaged must have 3 or 4 parts: value=effect:optionalNumber, count=integer:integer"
              + thisErrorMsg());
    }
    final int from = getInt(s[0]);
    final int to = getInt(s[1]);
    if (from < 0 || to < 0 || to < from) {
      throw new GameParseException("whenCombatDamaged damaged integers must be positive, and the second integer must "
          + "be equal to or greater than the first" + thisErrorMsg());
    }
    final Tuple<Integer, Integer> fromTo = Tuple.of(from, to);
    final Tuple<String, String> effectNum;
    if (s.length == 3) {
      effectNum = Tuple.of(s[2], null);
    } else {
      effectNum = Tuple.of(s[2], s[3]);
    }
    m_whenCombatDamaged.add(Tuple.of(fromTo, effectNum));
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setWhenCombatDamaged(final ArrayList<Tuple<Tuple<Integer, Integer>, Tuple<String, String>>> value) {
    m_whenCombatDamaged = value;
  }

  public ArrayList<Tuple<Tuple<Integer, Integer>, Tuple<String, String>>> getWhenCombatDamaged() {
    return m_whenCombatDamaged;
  }

  public void clearWhenCombatDamaged() {
    m_whenCombatDamaged.clear();
  }

  public void resetWhenCombatDamaged() {
    m_whenCombatDamaged = new ArrayList<>();
  }

  /**
   * Adds to, not sets. Anything that adds to instead of setting needs a clear function as well.
   */
  @GameProperty(xmlProperty = true, gameProperty = true, adds = true)
  public void setReceivesAbilityWhenWith(final String value) {
    m_receivesAbilityWhenWith.add(value);
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setReceivesAbilityWhenWith(final ArrayList<String> value) {
    m_receivesAbilityWhenWith = value;
  }

  public ArrayList<String> getReceivesAbilityWhenWith() {
    return m_receivesAbilityWhenWith;
  }

  public void clearReceivesAbilityWhenWith() {
    m_receivesAbilityWhenWith.clear();
  }

  public void resetReceivesAbilityWhenWith() {
    m_receivesAbilityWhenWith = new ArrayList<>();
  }

  private static IntegerMap<Tuple<String, String>> getReceivesAbilityWhenWithMap(final Collection<Unit> units,
      final String filterForAbility, final GameData data) {
    final IntegerMap<Tuple<String, String>> map = new IntegerMap<>();
    final Collection<UnitType> canReceive =
        getUnitTypesFromUnitList(Matches.getMatches(units, Matches.unitCanReceiveAbilityWhenWith()));
    for (final UnitType ut : canReceive) {
      final Collection<String> receives = UnitAttachment.get(ut).getReceivesAbilityWhenWith();
      for (final String receive : receives) {
        final String[] s = receive.split(":");
        if (filterForAbility != null && !filterForAbility.equals(s[0])) {
          continue;
        }
        map.put(Tuple.of(s[0], s[1]),
            Matches.countMatches(units, Matches.unitIsOfType(data.getUnitTypeList().getUnitType(s[1]))));
      }
    }
    return map;
  }

  public static Collection<Unit> getUnitsWhichReceivesAbilityWhenWith(final Collection<Unit> units,
      final String filterForAbility, final GameData data) {
    if (Match.noneMatch(units, Matches.unitCanReceiveAbilityWhenWith())) {
      return new ArrayList<>();
    }
    final Collection<Unit> unitsCopy = new ArrayList<>(units);
    final HashSet<Unit> whichReceiveNoDuplicates = new HashSet<>();
    final IntegerMap<Tuple<String, String>> whichGive =
        getReceivesAbilityWhenWithMap(unitsCopy, filterForAbility, data);
    for (final Tuple<String, String> abilityUnitType : whichGive.keySet()) {
      final Collection<Unit> receives = Matches.getNMatches(unitsCopy, whichGive.getInt(abilityUnitType),
          Matches.unitCanReceiveAbilityWhenWith(filterForAbility, abilityUnitType.getSecond()));
      whichReceiveNoDuplicates.addAll(receives);
      unitsCopy.removeAll(receives);
    }
    return whichReceiveNoDuplicates;
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setIsConstruction(final String s) {
    m_isConstruction = getBool(s);
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setIsConstruction(final Boolean s) {
    m_isConstruction = s;
  }

  public boolean getIsConstruction() {
    return m_isConstruction;
  }

  public void resetIsConstruction() {
    m_isConstruction = false;
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setConstructionType(final String s) {
    m_constructionType = s;
  }

  public String getConstructionType() {
    return m_constructionType;
  }

  public void resetConstructionType() {
    m_constructionType = "none";
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setConstructionsPerTerrPerTypePerTurn(final String s) {
    m_constructionsPerTerrPerTypePerTurn = getInt(s);
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setConstructionsPerTerrPerTypePerTurn(final Integer s) {
    m_constructionsPerTerrPerTypePerTurn = s;
  }

  public int getConstructionsPerTerrPerTypePerTurn() {
    return m_constructionsPerTerrPerTypePerTurn;
  }

  public void resetConstructionsPerTerrPerTypePerTurn() {
    m_constructionsPerTerrPerTypePerTurn = -1;
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setMaxConstructionsPerTypePerTerr(final String s) {
    m_maxConstructionsPerTypePerTerr = getInt(s);
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setMaxConstructionsPerTypePerTerr(final Integer s) {
    m_maxConstructionsPerTypePerTerr = s;
  }

  public int getMaxConstructionsPerTypePerTerr() {
    return m_maxConstructionsPerTypePerTerr;
  }

  public void resetMaxConstructionsPerTypePerTerr() {
    m_maxConstructionsPerTypePerTerr = -1;
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setIsMarine(final String s) {
    if (s.equalsIgnoreCase(Constants.PROPERTY_TRUE)) {
      m_isMarine = 1;
    } else if (s.equalsIgnoreCase(Constants.PROPERTY_FALSE)) {
      m_isMarine = 0;
    } else {
      m_isMarine = getInt(s);
    }
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setIsMarine(final Integer s) {
    m_isMarine = s;
  }

  public int getIsMarine() {
    return m_isMarine;
  }

  public void resetIsMarine() {
    m_isMarine = 0;
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setIsInfantry(final String s) {
    m_isInfantry = getBool(s);
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setIsInfantry(final Boolean s) {
    m_isInfantry = s;
  }

  public boolean getIsInfantry() {
    return m_isInfantry;
  }

  public void resetIsInfantry() {
    m_isInfantry = false;
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setIsLandTransport(final String s) {
    m_isLandTransport = getBool(s);
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setIsLandTransport(final Boolean s) {
    m_isLandTransport = s;
  }

  public boolean isLandTransport() {
    return m_isLandTransport;
  }

  public boolean getIsLandTransport() {
    return m_isLandTransport;
  }

  public void resetIsLandTransport() {
    m_isLandTransport = false;
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setTransportCapacity(final String s) {
    m_transportCapacity = getInt(s);
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setTransportCapacity(final Integer s) {
    m_transportCapacity = s;
  }

  public int getTransportCapacity() {
    return m_transportCapacity;
  }

  public void resetTransportCapacity() {
    m_transportCapacity = -1;
  }

  // DO NOT REMOVE.
  @GameProperty(xmlProperty = true, gameProperty = true, adds = false, virtual = true)
  public void setIsTwoHit(final String s) {
    m_hitPoints = getBool(s) ? 2 : 1;
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setHitPoints(final String s) {
    m_hitPoints = getInt(s);
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setHitPoints(final Integer value) {
    m_hitPoints = value;
  }

  public int getHitPoints() {
    return m_hitPoints;
  }

  public void resetHitPoints() {
    m_hitPoints = 1;
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setTransportCost(final String s) {
    m_transportCost = getInt(s);
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setTransportCost(final Integer s) {
    m_transportCost = s;
  }

  public int getTransportCost() {
    return m_transportCost;
  }

  public void resetTransportCost() {
    m_transportCost = -1;
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setMaxBuiltPerPlayer(final String s) {
    m_maxBuiltPerPlayer = getInt(s);
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setMaxBuiltPerPlayer(final Integer s) {
    m_maxBuiltPerPlayer = s;
  }

  public int getMaxBuiltPerPlayer() {
    return m_maxBuiltPerPlayer;
  }

  public void resetMaxBuiltPerPlayer() {
    m_maxBuiltPerPlayer = -1;
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setCarrierCapacity(final String s) {
    m_carrierCapacity = getInt(s);
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setCarrierCapacity(final Integer s) {
    m_carrierCapacity = s;
  }

  public int getCarrierCapacity() {
    return m_carrierCapacity;
  }

  public void resetCarrierCapacity() {
    m_carrierCapacity = -1;
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setCarrierCost(final String s) {
    m_carrierCost = getInt(s);
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setCarrierCost(final Integer s) {
    m_carrierCost = s;
  }

  public int getCarrierCost() {
    return m_carrierCost;
  }

  public void resetCarrierCost() {
    m_carrierCost = -1;
  }

  @GameProperty(xmlProperty = true, gameProperty = false, adds = false)
  public void setArtillery(final String s) throws GameParseException {
    m_artillery = getBool(s);
    if (m_artillery) {
      UnitSupportAttachment.addRule((UnitType) getAttachedTo(), getData(), false);
    }
  }

  @GameProperty(xmlProperty = true, gameProperty = false, adds = false)
  public void setArtillery(final Boolean s) throws GameParseException {
    m_artillery = s;
    if (m_artillery) {
      UnitSupportAttachment.addRule((UnitType) getAttachedTo(), getData(), false);
    }
  }

  public boolean getArtillery() {
    return m_artillery;
  }

  public void resetArtillery() {
    throw new IllegalStateException(
        "Resetting Artillery (UnitAttachment) is not allowed, please use Support Attachments instead.");
  }

  @GameProperty(xmlProperty = true, gameProperty = false, adds = false)
  public void setArtillerySupportable(final String s) throws GameParseException {
    m_artillerySupportable = getBool(s);
    if (m_artillerySupportable) {
      UnitSupportAttachment.addTarget((UnitType) getAttachedTo(), getData());
    }
  }

  @GameProperty(xmlProperty = true, gameProperty = false, adds = false)
  public void setArtillerySupportable(final Boolean s) throws GameParseException {
    m_artillerySupportable = s;
    if (m_artillerySupportable) {
      UnitSupportAttachment.addTarget((UnitType) getAttachedTo(), getData());
    }
  }

  public boolean getArtillerySupportable() {
    return m_artillerySupportable;
  }

  public void resetArtillerySupportable() {
    throw new IllegalStateException(
        "Resetting Artillery Supportable (UnitAttachment) is not allowed, please use Support Attachments instead.");
  }

  @GameProperty(xmlProperty = true, gameProperty = false, adds = false)
  public void setUnitSupportCount(final String s) {
    m_unitSupportCount = getInt(s);
    UnitSupportAttachment.setOldSupportCount((UnitType) getAttachedTo(), getData(), s);
  }

  @GameProperty(xmlProperty = true, gameProperty = false, adds = false)
  public void setUnitSupportCount(final Integer s) {
    m_unitSupportCount = s;
    UnitSupportAttachment.setOldSupportCount((UnitType) getAttachedTo(), getData(), s.toString());
  }

  public int getUnitSupportCount() {
    return m_unitSupportCount > 0 ? m_unitSupportCount : 1;
  }

  public void resetUnitSupportCount() {
    throw new IllegalStateException(
        "Resetting Artillery Support Count (UnitAttachment) is not allowed, please use Support Attachments instead.");
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setBombard(final String s) {
    m_bombard = getInt(s);
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setBombard(final Integer s) {
    m_bombard = s;
  }

  public int getBombard() {
    return m_bombard > 0 ? m_bombard : m_attack;
  }

  public void resetBombard() {
    m_bombard = -1;
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setMovement(final String s) {
    m_movement = getInt(s);
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setMovement(final Integer s) {
    m_movement = s;
  }

  public int getMovement() {
    return m_movement;
  }

  public int getMovement(final PlayerID player) {
    return Math.max(0,
        m_movement + TechAbilityAttachment.getMovementBonus((UnitType) this.getAttachedTo(), player, getData()));
  }

  public void resetMovement() {
    m_movement = 0;
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setAttack(final String s) {
    m_attack = getInt(s);
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setAttack(final int s) {
    m_attack = s;
  }

  public int getAttack() {
    return m_attack;
  }

  public int getAttack(final PlayerID player) {
    final int attackValue =
        m_attack + TechAbilityAttachment.getAttackBonus((UnitType) this.getAttachedTo(), player, getData());
    return Math.min(getData().getDiceSides(), Math.max(0, attackValue));
  }

  public void resetAttack() {
    m_attack = 0;
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setAttackRolls(final String s) {
    m_attackRolls = getInt(s);
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setAttackRolls(final Integer s) {
    m_attackRolls = s;
  }

  public int getAttackRolls() {
    return m_attackRolls;
  }

  public int getAttackRolls(final PlayerID player) {
    return Math.max(0,
        m_attackRolls + TechAbilityAttachment.getAttackRollsBonus((UnitType) this.getAttachedTo(), player, getData()));
  }

  public void resetAttackRolls() {
    m_attackRolls = 1;
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setDefense(final String s) {
    m_defense = getInt(s);
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setDefense(final Integer s) {
    m_defense = s;
  }

  public int getDefense() {
    return m_defense;
  }

  public int getDefense(final PlayerID player) {
    int defenseValue =
        m_defense + TechAbilityAttachment.getDefenseBonus((UnitType) this.getAttachedTo(), player, getData());
    if (defenseValue > 0 && m_isSub && TechTracker.hasSuperSubs(player)) {
      final int bonus = Properties.getSuper_Sub_Defense_Bonus(getData());
      defenseValue += bonus;
    }
    return Math.min(getData().getDiceSides(), Math.max(0, defenseValue));
  }

  public void resetDefense() {
    m_defense = 0;
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setDefenseRolls(final String s) {
    m_defenseRolls = getInt(s);
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setDefenseRolls(final Integer s) {
    m_defenseRolls = s;
  }

  public int getDefenseRolls() {
    return m_defenseRolls;
  }

  public int getDefenseRolls(final PlayerID player) {
    return Math.max(0, m_defenseRolls
        + TechAbilityAttachment.getDefenseRollsBonus((UnitType) this.getAttachedTo(), player, getData()));
  }

  public void resetDefenseRolls() {
    m_defenseRolls = 1;
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setChooseBestRoll(final String s) {
    m_chooseBestRoll = getBool(s);
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setChooseBestRoll(final Boolean s) {
    m_chooseBestRoll = s;
  }

  public boolean getChooseBestRoll() {
    return m_chooseBestRoll;
  }

  public void resetChooseBestRoll() {
    m_chooseBestRoll = false;
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setCanScramble(final String s) {
    m_canScramble = getBool(s);
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setCanScramble(final Boolean s) {
    m_canScramble = s;
  }

  public boolean getCanScramble() {
    return m_canScramble;
  }

  public void resetCanScramble() {
    m_canScramble = false;
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setMaxScrambleCount(final String s) {
    m_maxScrambleCount = getInt(s);
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setMaxScrambleCount(final Integer s) {
    m_maxScrambleCount = s;
  }

  public int getMaxScrambleCount() {
    return m_maxScrambleCount;
  }

  public void resetMaxScrambleCount() {
    m_maxScrambleCount = -1;
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setMaxScrambleDistance(final String s) {
    m_maxScrambleDistance = getInt(s);
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setMaxScrambleDistance(final Integer s) {
    m_maxScrambleDistance = s;
  }

  public int getMaxScrambleDistance() {
    return m_maxScrambleDistance;
  }

  public void resetMaxScrambleDistance() {
    m_maxScrambleDistance = -1;
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setMaxOperationalDamage(final String s) {
    m_maxOperationalDamage = getInt(s);
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setMaxOperationalDamage(final Integer s) {
    m_maxOperationalDamage = s;
  }

  public int getMaxOperationalDamage() {
    return m_maxOperationalDamage;
  }

  public void resetMaxOperationalDamage() {
    m_maxOperationalDamage = -1;
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setMaxDamage(final String s) {
    m_maxDamage = getInt(s);
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setMaxDamage(final Integer s) {
    m_maxDamage = s;
  }

  public int getMaxDamage() {
    return m_maxDamage;
  }

  public void resetMaxDamage() {
    m_maxDamage = 2;
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setIsAirBase(final String s) {
    m_isAirBase = getBool(s);
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setIsAirBase(final Boolean s) {
    m_isAirBase = s;
  }

  public boolean getIsAirBase() {
    return m_isAirBase;
  }

  public void resetIsAirBase() {
    m_isAirBase = false;
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setIsInfrastructure(final String s) {
    m_isInfrastructure = getBool(s);
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setIsInfrastructure(final Boolean s) {
    m_isInfrastructure = s;
  }

  public boolean getIsInfrastructure() {
    return m_isInfrastructure;
  }

  public void resetIsInfrastructure() {
    m_isInfrastructure = false;
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setCanBeDamaged(final String s) {
    m_canBeDamaged = getBool(s);
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setCanBeDamaged(final Boolean s) {
    m_canBeDamaged = s;
  }

  public boolean getCanBeDamaged() {
    return m_canBeDamaged;
  }

  public void resetCanBeDamaged() {
    m_canBeDamaged = false;
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setCanDieFromReachingMaxDamage(final String s) {
    m_canDieFromReachingMaxDamage = getBool(s);
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setCanDieFromReachingMaxDamage(final Boolean s) {
    m_canDieFromReachingMaxDamage = s;
  }

  public boolean getCanDieFromReachingMaxDamage() {
    return m_canDieFromReachingMaxDamage;
  }

  public void resetCanDieFromReachingMaxDamage() {
    m_canDieFromReachingMaxDamage = false;
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setIsSuicide(final String s) {
    m_isSuicide = getBool(s);
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setIsSuicide(final boolean s) {
    m_isSuicide = s;
  }

  public boolean getIsSuicide() {
    return m_isSuicide;
  }

  public void resetIsSuicide() {
    m_isSuicide = false;
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setIsKamikaze(final String s) {
    m_isKamikaze = getBool(s);
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setIsKamikaze(final boolean s) {
    m_isKamikaze = s;
  }

  public boolean getIsKamikaze() {
    return m_isKamikaze;
  }

  public void resetIsKamikaze() {
    m_isKamikaze = false;
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setBlockade(final String s) {
    m_blockade = getInt(s);
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setBlockade(final int s) {
    m_blockade = s;
  }

  public int getBlockade() {
    return m_blockade;
  }

  public void resetBlockade() {
    m_blockade = 0;
  }

  /**
   * Adds to, not sets. Anything that adds to instead of setting needs a clear function as well.
   */
  @GameProperty(xmlProperty = true, gameProperty = true, adds = true)
  public void setGivesMovement(final String value) throws GameParseException {
    final String[] s = value.split(":");
    if (s.length <= 0 || s.length > 2) {
      throw new GameParseException("givesMovement cannot be empty or have more than two fields" + thisErrorMsg());
    }
    final String unitTypeToProduce;
    unitTypeToProduce = s[1];
    // validate that this unit exists in the xml
    final UnitType ut = getData().getUnitTypeList().getUnitType(unitTypeToProduce);
    if (ut == null) {
      throw new GameParseException("No unit called:" + unitTypeToProduce + thisErrorMsg());
    }
    // we should allow positive and negative numbers, since you can give bonuses to units or take away a unit's movement
    final int n = getInt(s[0]);
    m_givesMovement.put(ut, n);
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setGivesMovement(final IntegerMap<UnitType> value) {
    m_givesMovement = value;
  }

  public IntegerMap<UnitType> getGivesMovement() {
    return m_givesMovement;
  }

  public void clearGivesMovement() {
    m_givesMovement.clear();
  }

  public void resetGivesMovement() {
    m_givesMovement = new IntegerMap<>();
  }

  /**
   * Adds to, not sets. Anything that adds to instead of setting needs a clear function as well.
   */
  @GameProperty(xmlProperty = true, gameProperty = true, adds = true)
  public void setConsumesUnits(final String value) throws GameParseException {
    final String[] s = value.split(":");
    if (s.length != 2) {
      throw new GameParseException("consumesUnits must have two fields" + thisErrorMsg());
    }
    final String unitTypeToProduce;
    unitTypeToProduce = s[1];
    // validate that this unit exists in the xml
    final UnitType ut = getData().getUnitTypeList().getUnitType(unitTypeToProduce);
    if (ut == null) {
      throw new GameParseException("No unit called:" + unitTypeToProduce + thisErrorMsg());
    }
    final int n = getInt(s[0]);
    if (n < 1) {
      throw new GameParseException("consumesUnits must have positive values" + thisErrorMsg());
    }
    m_consumesUnits.put(ut, n);
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setConsumesUnits(final IntegerMap<UnitType> value) {
    m_consumesUnits = value;
  }

  public IntegerMap<UnitType> getConsumesUnits() {
    return m_consumesUnits;
  }

  public void clearConsumesUnits() {
    m_consumesUnits.clear();
  }

  public void resetConsumesUnits() {
    m_consumesUnits = new IntegerMap<>();
  }

  /**
   * Adds to, not sets. Anything that adds to instead of setting needs a clear function as well.
   */
  @GameProperty(xmlProperty = true, gameProperty = true, adds = true)
  public void setCreatesUnitsList(final String value) throws GameParseException {
    final String[] s = value.split(":");
    if (s.length <= 0 || s.length > 2) {
      throw new GameParseException("createsUnitsList cannot be empty or have more than two fields" + thisErrorMsg());
    }
    final String unitTypeToProduce;
    unitTypeToProduce = s[1];
    // validate that this unit exists in the xml
    final UnitType ut = getData().getUnitTypeList().getUnitType(unitTypeToProduce);
    if (ut == null) {
      throw new GameParseException("createsUnitsList: No unit called:" + unitTypeToProduce + thisErrorMsg());
    }
    final int n = getInt(s[0]);
    if (n < 1) {
      throw new GameParseException("createsUnitsList must have positive values" + thisErrorMsg());
    }
    m_createsUnitsList.put(ut, n);
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setCreatesUnitsList(final IntegerMap<UnitType> value) {
    m_createsUnitsList = value;
  }

  public IntegerMap<UnitType> getCreatesUnitsList() {
    return m_createsUnitsList;
  }

  public void clearCreatesUnitsList() {
    m_createsUnitsList.clear();
  }

  public void resetCreatesUnitsList() {
    m_createsUnitsList = new IntegerMap<>();
  }

  /**
   * Adds to, not sets. Anything that adds to instead of setting needs a clear function as well.
   */
  @GameProperty(xmlProperty = true, gameProperty = true, adds = true)
  public void setCreatesResourcesList(final String value) throws GameParseException {
    final String[] s = value.split(":");
    if (s.length <= 0 || s.length > 2) {
      throw new GameParseException(
          "createsResourcesList cannot be empty or have more than two fields" + thisErrorMsg());
    }
    final String resourceToProduce;
    resourceToProduce = s[1];
    // validate that this resource exists in the xml
    final Resource r = getData().getResourceList().getResource(resourceToProduce);
    if (r == null) {
      throw new GameParseException("createsResourcesList: No resource called:" + resourceToProduce + thisErrorMsg());
    }
    final int n = getInt(s[0]);
    m_createsResourcesList.put(r, n);
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setCreatesResourcesList(final IntegerMap<Resource> value) {
    m_createsResourcesList = value;
  }

  public IntegerMap<Resource> getCreatesResourcesList() {
    return m_createsResourcesList;
  }

  public void clearCreatesResourcesList() {
    m_createsResourcesList.clear();
  }

  public void resetCreatesResourcesList() {
    m_createsResourcesList = new IntegerMap<>();
  }

  /**
   * Adds to, not sets. Anything that adds to instead of setting needs a clear function as well.
   */
  @GameProperty(xmlProperty = true, gameProperty = true, adds = true)
  public void setFuelCost(final String value) throws GameParseException {
    final String[] s = value.split(":");
    if (s.length != 2) {
      throw new GameParseException("fuelCost must have two fields" + thisErrorMsg());
    }
    final String resourceToProduce;
    resourceToProduce = s[1];
    // validate that this resource exists in the xml
    final Resource r = getData().getResourceList().getResource(resourceToProduce);
    if (r == null) {
      throw new GameParseException("fuelCost: No resource called:" + resourceToProduce + thisErrorMsg());
    }
    final int n = getInt(s[0]);
    if (n < 0) {
      throw new GameParseException("fuelCost must have positive values" + thisErrorMsg());
    }
    m_fuelCost.put(r, n);
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setFuelCost(final IntegerMap<Resource> value) {
    m_fuelCost = value;
  }

  public IntegerMap<Resource> getFuelCost() {
    return m_fuelCost;
  }

  public void clearFuelCost() {
    m_fuelCost.clear();
  }

  public void resetFuelCost() {
    m_fuelCost = new IntegerMap<>();
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setBombingBonus(final String s) {
    m_bombingBonus = getInt(s);
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setBombingBonus(final int s) {
    m_bombingBonus = s;
  }

  public int getBombingBonus() {
    return m_bombingBonus;
  }

  public void resetBombingBonus() {
    m_bombingBonus = -1;
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setBombingMaxDieSides(final String s) {
    m_bombingMaxDieSides = getInt(s);
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setBombingMaxDieSides(final int s) {
    m_bombingMaxDieSides = s;
  }

  public int getBombingMaxDieSides() {
    return m_bombingMaxDieSides;
  }

  public void resetBombingMaxDieSides() {
    m_bombingMaxDieSides = -1;
  }

  /**
   * Adds to, not sets. Anything that adds to instead of setting needs a clear function as well.
   */
  @GameProperty(xmlProperty = true, gameProperty = true, adds = true)
  public void setBombingTargets(final String value) throws GameParseException {
    if (value == null) {
      m_bombingTargets = null;
      return;
    }
    if (m_bombingTargets == null) {
      m_bombingTargets = new HashSet<>();
    }
    final String[] s = value.split(":");
    for (final String u : s) {
      final UnitType ut = getData().getUnitTypeList().getUnitType(u);
      if (ut == null) {
        throw new GameParseException("bombingTargets: no such unit type: " + u + thisErrorMsg());
      }
      m_bombingTargets.add(ut);
    }
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setBombingTargets(final HashSet<UnitType> value) {
    m_bombingTargets = value;
  }

  public HashSet<UnitType> getBombingTargets() {
    return m_bombingTargets;
  }

  public HashSet<UnitType> getBombingTargets(final GameData data) {
    if (m_bombingTargets != null) {
      return m_bombingTargets;
    }
    return new HashSet<>(data.getUnitTypeList().getAllUnitTypes());
  }

  public void clearBombingTargets() {
    m_bombingTargets.clear();
  }

  public void resetBombingTargets() {
    m_bombingTargets = null;
  }

  public static Set<UnitType> getAllowedBombingTargetsIntersection(final Collection<Unit> bombersOrRockets,
      final GameData data) {
    if (bombersOrRockets.isEmpty()) {
      return new HashSet<>();
    }
    Collection<UnitType> allowedTargets = data.getUnitTypeList().getAllUnitTypes();
    for (final Unit u : bombersOrRockets) {
      final UnitAttachment ua = UnitAttachment.get(u.getType());
      final HashSet<UnitType> bombingTargets = ua.getBombingTargets(data);
      if (bombingTargets != null) {
        allowedTargets = Util.intersection(allowedTargets, bombingTargets);
      }
    }
    return new HashSet<>(allowedTargets);
  }

  // Do not delete, we keep this both for backwards compatibility, and for user convenience when making maps
  @GameProperty(xmlProperty = true, gameProperty = true, adds = false, virtual = true)
  public void setIsAA(final String s) throws GameParseException {
    setIsAA(getBool(s));
  }

  // Do not delete, we keep this both for backwards compatibility, and for user convenience when making maps
  @GameProperty(xmlProperty = true, gameProperty = true, adds = false, virtual = true)
  public void setIsAA(final Boolean s) throws GameParseException {
    setIsAAforCombatOnly(s);
    setIsAAforBombingThisUnitOnly(s);
    setIsAAforFlyOverOnly(s);
    setIsAAmovement(s);
    setIsRocket(s);
    setIsInfrastructure(s);
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setAttackAA(final String s) {
    m_attackAA = getInt(s);
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setAttackAA(final int s) {
    m_attackAA = s;
  }

  public int getAttackAA() {
    return m_attackAA;
  }

  public int getAttackAA(final PlayerID player) {
    // TODO: this may cause major problems with Low Luck, if they have diceSides equal to something other than 6, or it
    // does not divide
    // perfectly into attackAAmaxDieSides
    return Math.max(0, Math.min(getAttackAAmaxDieSides(),
        m_attackAA + TechAbilityAttachment.getRadarBonus((UnitType) this.getAttachedTo(), player, getData())));
  }

  public void resetAttackAA() {
    m_attackAA = 1;
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setOffensiveAttackAA(final String s) {
    m_offensiveAttackAA = getInt(s);
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setOffensiveAttackAA(final Integer s) {
    m_offensiveAttackAA = s;
  }

  public int getOffensiveAttackAA() {
    return m_offensiveAttackAA;
  }

  public int getOffensiveAttackAA(final PlayerID player) {
    // TODO: this may cause major problems with Low Luck, if they have diceSides equal to something other than 6, or it
    // does not divide
    // perfectly into attackAAmaxDieSides
    return Math.max(0, Math.min(getOffensiveAttackAAmaxDieSides(),
        m_offensiveAttackAA + TechAbilityAttachment.getRadarBonus((UnitType) this.getAttachedTo(), player, getData())));
  }

  public void resetOffensiveAttackAA() {
    m_offensiveAttackAA = 1;
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setAttackAAmaxDieSides(final String s) {
    m_attackAAmaxDieSides = getInt(s);
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setAttackAAmaxDieSides(final Integer s) {
    m_attackAAmaxDieSides = s;
  }

  public int getAttackAAmaxDieSides() {
    if (m_attackAAmaxDieSides < 0) {
      return getData().getDiceSides();
    }
    return m_attackAAmaxDieSides;
  }

  public void resetAttackAAmaxDieSides() {
    m_attackAAmaxDieSides = -1;
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setOffensiveAttackAAmaxDieSides(final String s) {
    m_offensiveAttackAAmaxDieSides = getInt(s);
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setOffensiveAttackAAmaxDieSides(final Integer s) {
    m_offensiveAttackAAmaxDieSides = s;
  }

  public int getOffensiveAttackAAmaxDieSides() {
    if (m_offensiveAttackAAmaxDieSides < 0) {
      return getData().getDiceSides();
    }
    return m_offensiveAttackAAmaxDieSides;
  }

  public void resetOffensiveAttackAAmaxDieSides() {
    m_offensiveAttackAAmaxDieSides = -1;
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setMaxAAattacks(final String s) throws GameParseException {
    final int attacks = getInt(s);
    if (attacks < -1) {
      throw new GameParseException("maxAAattacks must be positive (or -1 for attacking all) " + thisErrorMsg());
    }
    m_maxAAattacks = getInt(s);
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setMaxAAattacks(final Integer s) {
    m_maxAAattacks = s;
  }

  public int getMaxAAattacks() {
    return m_maxAAattacks;
  }

  public void resetMaxAAattacks() {
    m_maxAAattacks = -1;
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setMaxRoundsAA(final String s) throws GameParseException {
    final int attacks = getInt(s);
    if (attacks < -1) {
      throw new GameParseException("maxRoundsAA must be positive (or -1 for infinite) " + thisErrorMsg());
    }
    m_maxRoundsAA = getInt(s);
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setMaxRoundsAA(final Integer s) {
    m_maxRoundsAA = s;
  }

  public int getMaxRoundsAA() {
    return m_maxRoundsAA;
  }

  public void resetMaxRoundsAA() {
    m_maxRoundsAA = 1;
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setMayOverStackAA(final String s) {
    m_mayOverStackAA = getBool(s);
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setMayOverStackAA(final Boolean s) {
    m_mayOverStackAA = s;
  }

  public boolean getMayOverStackAA() {
    return m_mayOverStackAA;
  }

  public void resetMayOverStackAA() {
    m_mayOverStackAA = false;
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setDamageableAA(final String s) {
    m_damageableAA = getBool(s);
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setDamageableAA(final Boolean s) {
    m_damageableAA = s;
  }

  public boolean getDamageableAA() {
    return m_damageableAA;
  }

  public void resetDamageableAA() {
    m_damageableAA = false;
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setIsAAforCombatOnly(final String s) {
    m_isAAforCombatOnly = getBool(s);
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setIsAAforCombatOnly(final Boolean s) {
    m_isAAforCombatOnly = s;
  }

  public boolean getIsAAforCombatOnly() {
    return m_isAAforCombatOnly;
  }

  public void resetIsAAforCombatOnly() {
    m_isAAforCombatOnly = false;
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setIsAAforBombingThisUnitOnly(final String s) {
    m_isAAforBombingThisUnitOnly = getBool(s);
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setIsAAforBombingThisUnitOnly(final Boolean s) {
    m_isAAforBombingThisUnitOnly = s;
  }

  public boolean getIsAAforBombingThisUnitOnly() {
    return m_isAAforBombingThisUnitOnly;
  }

  public void resetIsAAforBombingThisUnitOnly() {
    m_isAAforBombingThisUnitOnly = false;
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setIsAAforFlyOverOnly(final String s) {
    m_isAAforFlyOverOnly = getBool(s);
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setIsAAforFlyOverOnly(final Boolean s) {
    m_isAAforFlyOverOnly = s;
  }

  public boolean getIsAAforFlyOverOnly() {
    return m_isAAforFlyOverOnly;
  }

  public void resetIsAAforFlyOverOnly() {
    m_isAAforFlyOverOnly = false;
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setIsRocket(final String s) {
    m_isRocket = getBool(s);
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setIsRocket(final Boolean s) {
    m_isRocket = s;
  }

  public boolean getIsRocket() {
    return m_isRocket;
  }

  public void resetIsRocket() {
    m_isRocket = false;
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setTypeAA(final String s) {
    m_typeAA = s;
  }

  public String getTypeAA() {
    return m_typeAA;
  }

  public void resetTypeAA() {
    m_typeAA = "AA";
  }

  public static List<String> getAllOfTypeAAs(final Collection<Unit> aaUnitsAlreadyVerified) {
    final Set<String> aaSet = new HashSet<>();
    for (final Unit u : aaUnitsAlreadyVerified) {
      aaSet.add(UnitAttachment.get(u.getType()).getTypeAA());
    }
    final List<String> aaTypes = new ArrayList<>(aaSet);
    Collections.sort(aaTypes);
    return aaTypes;
  }

  /**
   * Adds to, not sets. Anything that adds to instead of setting needs a clear function as well.
   */
  @GameProperty(xmlProperty = true, gameProperty = true, adds = true)
  public void setTargetsAA(final String value) throws GameParseException {
    if (value == null) {
      m_targetsAA = null;
      return;
    }
    if (m_targetsAA == null) {
      m_targetsAA = new HashSet<>();
    }
    final String[] s = value.split(":");
    for (final String u : s) {
      final UnitType ut = getData().getUnitTypeList().getUnitType(u);
      if (ut == null) {
        throw new GameParseException("AAtargets: no such unit type: " + u + thisErrorMsg());
      }
      m_targetsAA.add(ut);
    }
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setTargetsAA(final HashSet<UnitType> value) {
    m_targetsAA = value;
  }

  public HashSet<UnitType> getTargetsAA() {
    return m_targetsAA;
  }

  public HashSet<UnitType> getTargetsAA(final GameData data) {
    if (m_targetsAA != null) {
      return m_targetsAA;
    }
    final HashSet<UnitType> airTypes = new HashSet<>();
    final Iterator<UnitType> utIter = data.getUnitTypeList().iterator();
    while (utIter.hasNext()) {
      final UnitType ut = utIter.next();
      if (UnitAttachment.get(ut).getIsAir()) {
        airTypes.add(ut);
      }
    }
    return airTypes;
  }

  public void clearTargetsAA() {
    m_targetsAA.clear();
  }

  public void resetTargetsAA() {
    m_targetsAA = null;
  }

  /**
   * Adds to, not sets. Anything that adds to instead of setting needs a clear function as well.
   */
  @GameProperty(xmlProperty = true, gameProperty = true, adds = true)
  public void setWillNotFireIfPresent(final String value) throws GameParseException {
    final String[] s = value.split(":");
    for (final String u : s) {
      final UnitType ut = getData().getUnitTypeList().getUnitType(u);
      if (ut == null) {
        throw new GameParseException("willNotFireIfPresent: no such unit type: " + u + thisErrorMsg());
      }
      m_willNotFireIfPresent.add(ut);
    }
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setWillNotFireIfPresent(final HashSet<UnitType> value) {
    m_willNotFireIfPresent = value;
  }

  public HashSet<UnitType> getWillNotFireIfPresent() {
    return m_willNotFireIfPresent;
  }

  public void clearWillNotFireIfPresent() {
    m_willNotFireIfPresent.clear();
  }

  public void resetWillNotFireIfPresent() {
    m_willNotFireIfPresent = new HashSet<>();
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false, virtual = true)
  public void setIsAAmovement(final String s) throws GameParseException {
    setIsAAmovement(getBool(s));
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false, virtual = true)
  public void setIsAAmovement(final Boolean s) throws GameParseException {
    setCanNotMoveDuringCombatMove(s);
    if (s) {
      setMovementLimit(Integer.MAX_VALUE + ":allied");
      setAttackingLimit(Integer.MAX_VALUE + ":allied");
      setPlacementLimit(Integer.MAX_VALUE + ":allied");
    } else {
      m_movementLimit = null;
      m_attackingLimit = null;
      m_placementLimit = null;
    }
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setCanNotMoveDuringCombatMove(final String s) {
    m_canNotMoveDuringCombatMove = getBool(s);
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setCanNotMoveDuringCombatMove(final Boolean s) {
    m_canNotMoveDuringCombatMove = s;
  }

  public boolean getCanNotMoveDuringCombatMove() {
    return m_canNotMoveDuringCombatMove;
  }

  public void resetCanNotMoveDuringCombatMove() {
    m_canNotMoveDuringCombatMove = false;
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setMovementLimit(final String value) throws GameParseException {
    if (value == null) {
      m_movementLimit = null;
      return;
    }
    final UnitType ut = (UnitType) this.getAttachedTo();
    if (ut == null) {
      throw new GameParseException("getAttachedTo returned null" + thisErrorMsg());
    }
    final String[] s = value.split(":");
    if (s.length != 2) {
      throw new GameParseException("movementLimit must have 2 fields, value and count" + thisErrorMsg());
    }
    final int max = getInt(s[0]);
    if (max < 0) {
      throw new GameParseException("movementLimit count must have a positive number" + thisErrorMsg());
    }
    if (!(s[1].equals("owned") || s[1].equals("allied") || s[1].equals("total"))) {
      throw new GameParseException("movementLimit value must owned, allied, or total" + thisErrorMsg());
    }
    m_movementLimit = Tuple.of(max, s[1]);
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setMovementLimit(final Tuple<Integer, String> value) {
    m_movementLimit = value;
  }

  public Tuple<Integer, String> getMovementLimit() {
    return m_movementLimit;
  }

  public void resetMovementLimit() {
    m_movementLimit = null;
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setAttackingLimit(final String value) throws GameParseException {
    if (value == null) {
      m_attackingLimit = null;
      return;
    }
    final UnitType ut = (UnitType) this.getAttachedTo();
    if (ut == null) {
      throw new GameParseException("getAttachedTo returned null" + thisErrorMsg());
    }
    final String[] s = value.split(":");
    if (s.length != 2) {
      throw new GameParseException("attackingLimit must have 2 fields, value and count" + thisErrorMsg());
    }
    final int max = getInt(s[0]);
    if (max < 0) {
      throw new GameParseException("attackingLimit count must have a positive number" + thisErrorMsg());
    }
    if (!(s[1].equals("owned") || s[1].equals("allied") || s[1].equals("total"))) {
      throw new GameParseException("attackingLimit value must owned, allied, or total" + thisErrorMsg());
    }
    m_attackingLimit = Tuple.of(max, s[1]);
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setAttackingLimit(final Tuple<Integer, String> value) {
    m_attackingLimit = value;
  }

  public Tuple<Integer, String> getAttackingLimit() {
    return m_attackingLimit;
  }

  public void resetAttackingLimit() {
    m_attackingLimit = null;
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setPlacementLimit(final String value) throws GameParseException {
    if (value == null) {
      m_placementLimit = null;
      return;
    }
    final UnitType ut = (UnitType) this.getAttachedTo();
    if (ut == null) {
      throw new GameParseException("getAttachedTo returned null" + thisErrorMsg());
    }
    final String[] s = value.split(":");
    if (s.length != 2) {
      throw new GameParseException("placementLimit must have 2 fields, value and count" + thisErrorMsg());
    }
    final int max = getInt(s[0]);
    if (max < 0) {
      throw new GameParseException("placementLimit count must have a positive number" + thisErrorMsg());
    }
    if (!(s[1].equals("owned") || s[1].equals("allied") || s[1].equals("total"))) {
      throw new GameParseException("placementLimit value must owned, allied, or total" + thisErrorMsg());
    }
    m_placementLimit = Tuple.of(max, s[1]);
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setPlacementLimit(final Tuple<Integer, String> value) {
    m_placementLimit = value;
  }

  public Tuple<Integer, String> getPlacementLimit() {
    return m_placementLimit;
  }

  public void resetPlacementLimit() {
    m_placementLimit = null;
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setTuv(final String s) {
    m_tuv = getInt(s);
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setTuv(final Integer s) {
    m_tuv = s;
  }

  public int getTuv() {
    return m_tuv;
  }

  public void resetTuv() {
    m_tuv = -1;
  }

  public static int getMaximumNumberOfThisUnitTypeToReachStackingLimit(final String limitType, final UnitType ut,
      final Territory t, final PlayerID owner, final GameData data) {
    final UnitAttachment ua = UnitAttachment.get(ut);
    final Tuple<Integer, String> stackingLimit;
    if (limitType.equals("movementLimit")) {
      stackingLimit = ua.getMovementLimit();
    } else if (limitType.equals("attackingLimit")) {
      stackingLimit = ua.getAttackingLimit();
    } else if (limitType.equals("placementLimit")) {
      stackingLimit = ua.getPlacementLimit();
    } else {
      throw new IllegalStateException(
          "getMaximumNumberOfThisUnitTypeToReachStackingLimit does not allow limitType: " + limitType);
    }
    if (stackingLimit == null) {
      return Integer.MAX_VALUE;
    }
    int max = stackingLimit.getFirst();
    if (max == Integer.MAX_VALUE && (ua.getIsAAforBombingThisUnitOnly() || ua.getIsAAforCombatOnly())) {
      // under certain rules (classic rules) there can only be 1 aa gun in a territory.
      if (!(Properties.getWW2V2(data) || Properties.getWW2V3(data)
          || Properties.getMultipleAAPerTerritory(data))) {
        max = 1;
      }
    }
    final Match.CompositeBuilder<Unit> stackingMatchBuilder = Match.newCompositeBuilder(
        Matches.unitIsOfType(ut));
    final String stackingType = stackingLimit.getSecond();
    if (stackingType.equals("owned")) {
      stackingMatchBuilder.add(Matches.unitIsOwnedBy(owner));
    } else if (stackingType.equals("allied")) {
      stackingMatchBuilder.add(Matches.isUnitAllied(owner, data));
    }
    // else if (stackingType.equals("total"))
    final int totalInTerritory = Matches.countMatches(t.getUnits().getUnits(), stackingMatchBuilder.all());
    return Math.max(0, max - totalInTerritory);
  }

  @Override
  public void validate(final GameData data) throws GameParseException {
    if (m_isAir) {
      if (m_isSea /* || m_isFactory */ || m_isSub || m_transportCost != -1 || m_carrierCapacity != -1 || m_canBlitz
          || m_canBombard || m_isMarine != 0 || m_isInfantry || m_isLandTransport || m_isAirTransportable
          || m_isCombatTransport) {
        throw new GameParseException("air units cannot have certain properties, " + thisErrorMsg());
      }
    } else if (m_isSea) {
      if (m_canBlitz || m_isAir /* || m_isFactory */ || m_isStrategicBomber || m_carrierCost != -1
          || m_transportCost != -1 || m_isMarine != 0 || m_isInfantry || m_isLandTransport || m_isAirTransportable
          || m_isAirTransport || m_isKamikaze) {
        throw new GameParseException("sea units cannot have certain properties, " + thisErrorMsg());
      }
    } else { // if land
      if (m_canBombard || m_isStrategicBomber || m_isSub || m_carrierCapacity != -1 || m_bombard != -1
          || m_transportCapacity != -1 || m_isAirTransport || m_isCombatTransport || m_isKamikaze) {
        throw new GameParseException("land units cannot have certain properties, " + thisErrorMsg());
      }
    }
    if (m_hitPoints < 1) {
      throw new GameParseException("hitPoints cannot be zero or negative, " + thisErrorMsg());
    }
    if (m_attackAA < 0 || m_attackAAmaxDieSides < -1 || m_attackAAmaxDieSides > 200 || m_offensiveAttackAA < 0
        || m_offensiveAttackAAmaxDieSides < -1 || m_offensiveAttackAAmaxDieSides > 200) {
      throw new GameParseException(
          "attackAA or attackAAmaxDieSides or offensiveAttackAA or offensiveAttackAAmaxDieSides is wrong, "
              + thisErrorMsg());
    }
    if (m_carrierCapacity != -1 && m_carrierCost != -1) {
      throw new GameParseException("carrierCost and carrierCapacity cannot be set at same time, " + thisErrorMsg());
    }
    if (m_transportCost != -1 && m_transportCapacity != -1) {
      throw new GameParseException(
          "transportCost and transportCapacity cannot be set at same time, " + thisErrorMsg());
    }
    if (((m_bombingBonus >= 0 || m_bombingMaxDieSides >= 0) && !(m_isStrategicBomber || m_isRocket))
        || (m_bombingBonus < -1 || m_bombingMaxDieSides < -1)
        || (m_bombingBonus > 10000 || m_bombingMaxDieSides > 200)) {
      throw new GameParseException("something wrong with bombingBonus or bombingMaxDieSides, " + thisErrorMsg());
    }
    if (m_maxBuiltPerPlayer < -1) {
      throw new GameParseException("maxBuiltPerPlayer cannot be negative, " + thisErrorMsg());
    }
    if (m_isCombatTransport && m_transportCapacity < 1) {
      throw new GameParseException(
          "cannot have isCombatTransport on unit without transportCapacity, " + thisErrorMsg());
    }
    if (m_isSea && m_transportCapacity != -1 && Properties.getTransportCasualtiesRestricted(data)
        && (m_attack > 0 || m_defense > 0) && !m_isCombatTransport) {
      throw new GameParseException("Restricted transports cannot have attack or defense, " + thisErrorMsg());
    }
    if (m_isConstruction
        && (m_constructionType == null || m_constructionType.equals("none") || m_constructionType.equals("")
            || m_constructionsPerTerrPerTypePerTurn < 0 || m_maxConstructionsPerTypePerTerr < 0)) {
      throw new GameParseException("Constructions must have constructionType and positive constructionsPerTerrPerType "
          + "and maxConstructionsPerType, " + thisErrorMsg());
    }
    if (!m_isConstruction
        && (!(m_constructionType == null || m_constructionType.equals("none") || m_constructionType.equals(""))
            || m_constructionsPerTerrPerTypePerTurn >= 0 || m_maxConstructionsPerTypePerTerr >= 0)) {
      throw new GameParseException("Constructions must have isConstruction true, " + thisErrorMsg());
    }
    if (m_constructionsPerTerrPerTypePerTurn > m_maxConstructionsPerTypePerTerr) {
      throw new GameParseException(
          "Constructions must have constructionsPerTerrPerTypePerTurn Less than maxConstructionsPerTypePerTerr, "
              + thisErrorMsg());
    }
    if (m_unitPlacementRestrictions != null) {
      getListedTerritories(m_unitPlacementRestrictions);
    }
    if (m_requiresUnits != null) {
      for (final String[] combo : m_requiresUnits) {
        getListedUnits(combo);
      }
    }
    if ((m_canBeDamaged && m_maxDamage < 1) || (m_canDieFromReachingMaxDamage && m_maxDamage < 1)
        || (!m_canBeDamaged && m_canDieFromReachingMaxDamage)) {
      throw new GameParseException(
          "something wrong with canBeDamaged or maxDamage or canDieFromReachingMaxDamage or isFactory, "
              + thisErrorMsg());
    }
    if (m_canInvadeOnlyFrom != null && !m_canInvadeOnlyFrom[0].equals("all")
        && !m_canInvadeOnlyFrom[0].equals("none")) {
      for (final String transport : m_canInvadeOnlyFrom) {
        final UnitType ut = getData().getUnitTypeList().getUnitType(transport);
        if (ut == null) {
          throw new GameParseException("No unit called:" + transport + thisErrorMsg());
        }
        if (ut.getAttachments() == null || ut.getAttachments().isEmpty()) {
          throw new GameParseException(transport + " has no attachments, please declare " + transport
              + " in the xml before using it as a transport" + thisErrorMsg());
          // Units may be considered transported if they are on a carrier, or if they are paratroopers, or if they are
          // mech infantry. The
          // "transporter" may not be an actual transport, so we should not check for that here.
        }
      }
    }
    if (!m_receivesAbilityWhenWith.isEmpty()) {
      for (final String value : m_receivesAbilityWhenWith) {
        // first is ability, second is unit that we get it from
        final String[] s = value.split(":");
        if (s.length != 2) {
          throw new GameParseException("receivesAbilityWhenWith must have 2 parts, 'ability:unit'" + thisErrorMsg());
        }
        if (getData().getUnitTypeList().getUnitType(s[1]) == null) {
          throw new GameParseException("receivesAbilityWhenWith, unit does not exist, name:" + s[1] + thisErrorMsg());
        }
        // currently only supports canBlitz (m_canBlitz)
        if (!s[0].equals("canBlitz")) {
          throw new GameParseException("receivesAbilityWhenWith so far only supports: canBlitz" + thisErrorMsg());
        }
      }
    }
    if (!m_whenCombatDamaged.isEmpty()) {
      for (final Tuple<Tuple<Integer, Integer>, Tuple<String, String>> key : m_whenCombatDamaged) {
        final String obj = key.getSecond().getFirst();
        if (obj.equals(UNITSMAYNOTLANDONCARRIER)) {
          continue;
        }
        if (obj.equals(UNITSMAYNOTLEAVEALLIEDCARRIER)) {
          continue;
        }
        throw new GameParseException("m_whenCombatDamaged so far only supports: " + UNITSMAYNOTLANDONCARRIER + ", "
            + UNITSMAYNOTLEAVEALLIEDCARRIER + thisErrorMsg());
      }
    }
  }

  public Collection<UnitType> getListedUnits(final String[] list) {
    final List<UnitType> unitTypes = new ArrayList<>();
    for (final String name : list) {
      // Validate all units exist
      final UnitType ut = getData().getUnitTypeList().getUnitType(name);
      if (ut == null) {
        throw new IllegalStateException("No unit called: " + name + thisErrorMsg());
      }
      unitTypes.add(ut);
    }
    return unitTypes;
  }

  private Collection<Territory> getListedTerritories(final String[] list) throws GameParseException {
    final List<Territory> territories = new ArrayList<>();
    for (final String name : list) {
      // Validate all territories exist
      final Territory territory = getData().getMap().getTerritory(name);
      if (territory == null) {
        throw new GameParseException("No territory called: " + name + thisErrorMsg());
      }
      territories.add(territory);
    }
    return territories;
  }

  private static boolean playerHasRockets(final PlayerID player) {
    final TechAttachment ta = (TechAttachment) player.getAttachment(Constants.TECH_ATTACHMENT_NAME);
    if (ta == null) {
      return false;
    }
    return ta.getRocket();
  }

  private static boolean playerHasMechInf(final PlayerID player) {
    final TechAttachment ta = (TechAttachment) player.getAttachment(Constants.TECH_ATTACHMENT_NAME);
    if (ta == null) {
      return false;
    }
    return ta.getMechanizedInfantry();
  }

  private static boolean playerHasParatroopers(final PlayerID player) {
    final TechAttachment ta = (TechAttachment) player.getAttachment(Constants.TECH_ATTACHMENT_NAME);
    if (ta == null) {
      return false;
    }
    return ta.getParatroopers();
  }

  @Override
  public String toString() {
    // Any overriding method for toString on an attachment needs to include at least the Class, m_attachedTo, and
    // m_name. Or call
    // super.toString()
    return super.toString();
  }

  public String allUnitStatsForExporter() {
    // should cover ALL fields stored in UnitAttachment
    // remember to test for null and fix arrays
    // the stats exporter relies on this toString having two spaces after each entry, so do not change this please,
    // except to add new abilities onto the end
    return this.getAttachedTo().toString().replaceFirst("games.strategy.engine.data.", "") + " with:" + "  isAir:"
        + m_isAir + "  isSea:" + m_isSea + "  movement:" + m_movement + "  attack:" + m_attack + "  defense:"
        + m_defense + "  hitPoints:" + m_hitPoints
        + "  canBlitz:" + m_canBlitz + "  artillerySupportable:" + m_artillerySupportable + "  artillery:" + m_artillery
        + "  unitSupportCount:" + m_unitSupportCount + "  attackRolls:" + m_attackRolls + "  defenseRolls:"
        + m_defenseRolls + "  chooseBestRoll:" + m_chooseBestRoll + "  isMarine:" + m_isMarine + "  isInfantry:"
        + m_isInfantry + "  isLandTransport:" + m_isLandTransport + "  isAirTransportable:" + m_isAirTransportable
        + "  isAirTransport:" + m_isAirTransport + "  isStrategicBomber:" + m_isStrategicBomber + "  transportCapacity:"
        + m_transportCapacity + "  transportCost:" + m_transportCost + "  carrierCapacity:" + m_carrierCapacity
        + "  carrierCost:" + m_carrierCost + "  isSub:" + m_isSub + "  isDestroyer:" + m_isDestroyer + "  canBombard:"
        + m_canBombard + "  bombard:" + m_bombard + "  isAAforCombatOnly:" + m_isAAforCombatOnly
        + "  isAAforBombingThisUnitOnly:" + m_isAAforBombingThisUnitOnly + "  isAAforFlyOverOnly:"
        + m_isAAforFlyOverOnly + "  attackAA:" + m_attackAA + "  offensiveAttackAA:" + m_offensiveAttackAA
        + "  attackAAmaxDieSides:" + m_attackAAmaxDieSides + "  offensiveAttackAAmaxDieSides:"
        + m_offensiveAttackAAmaxDieSides + "  maxAAattacks:" + m_maxAAattacks + "  maxRoundsAA:" + m_maxRoundsAA
        + "  mayOverStackAA:" + m_mayOverStackAA + "  damageableAA:" + m_damageableAA + "  typeAA:" + m_typeAA
        + "  targetsAA:"
        + (m_targetsAA != null ? (m_targetsAA.size() == 0 ? "empty" : m_targetsAA.toString()) : "all air units")
        + "  willNotFireIfPresent:"
        + (m_willNotFireIfPresent != null
            ? (m_willNotFireIfPresent.size() == 0 ? "empty" : m_willNotFireIfPresent.toString())
            : "null")
        + "  isRocket:" + m_isRocket + "  canProduceUnits:" + m_canProduceUnits + "  canProduceXUnits:"
        + m_canProduceXUnits + "  createsUnitsList:"
        + (m_createsUnitsList != null ? (m_createsUnitsList.size() == 0 ? "empty" : m_createsUnitsList.toString())
            : "null")
        + "  createsResourcesList:"
        + (m_createsResourcesList != null
            ? (m_createsResourcesList.size() == 0 ? "empty" : m_createsResourcesList.toString())
            : "null")
        + "  fuelCost:" + (m_fuelCost != null ? (m_fuelCost.size() == 0 ? "empty" : m_fuelCost.toString()) : "null")
        + "  isInfrastructure:" + m_isInfrastructure + "  isConstruction:" + m_isConstruction + "  constructionType:"
        + m_constructionType + "  constructionsPerTerrPerTypePerTurn:" + m_constructionsPerTerrPerTypePerTurn
        + "  maxConstructionsPerTypePerTerr:" + m_maxConstructionsPerTypePerTerr + "  destroyedWhenCapturedBy:"
        + (m_destroyedWhenCapturedBy != null
            ? (m_destroyedWhenCapturedBy.size() == 0 ? "empty" : m_destroyedWhenCapturedBy.toString())
            : "null")
        + "  canBeCapturedOnEnteringBy:"
        + (m_canBeCapturedOnEnteringBy != null
            ? (m_canBeCapturedOnEnteringBy.size() == 0 ? "empty" : m_canBeCapturedOnEnteringBy.toString())
            : "null")
        + "  canBeDamaged:" + m_canBeDamaged + "  canDieFromReachingMaxDamage:" + m_canDieFromReachingMaxDamage
        + "  maxOperationalDamage:" + m_maxOperationalDamage + "  maxDamage:" + m_maxDamage
        + "  unitPlacementRestrictions:"
        + (m_unitPlacementRestrictions != null
            ? (m_unitPlacementRestrictions.length == 0 ? "empty" : Arrays.toString(m_unitPlacementRestrictions))
            : "null")
        + "  requiresUnits:"
        + (m_requiresUnits != null
            ? (m_requiresUnits.size() == 0 ? "empty" : MyFormatter.listOfArraysToString(m_requiresUnits))
            : "null")
        + "  consumesUnits:"
        + (m_consumesUnits != null ? (m_consumesUnits.size() == 0 ? "empty" : m_consumesUnits.toString()) : "null")
        + "  requiresUnitsToMove:"
        + (m_requiresUnitsToMove != null
            ? (m_requiresUnitsToMove.size() == 0 ? "empty" : MyFormatter.listOfArraysToString(m_requiresUnitsToMove))
            : "null")
        + "  canOnlyBePlacedInTerritoryValuedAtX:" + m_canOnlyBePlacedInTerritoryValuedAtX + "  maxBuiltPerPlayer:"
        + m_maxBuiltPerPlayer + "  special:"
        + (m_special != null ? (m_special.size() == 0 ? "empty" : m_special.toString()) : "null") + "  isSuicide:"
        + m_isSuicide + "  isSuicide:" + m_isSuicide + "  isCombatTransport:" + m_isCombatTransport
        + "  canInvadeOnlyFrom:"
        + (m_canInvadeOnlyFrom != null
            ? (m_canInvadeOnlyFrom.length == 0 ? "empty" : Arrays.toString(m_canInvadeOnlyFrom))
            : "null")
        + "  canBeGivenByTerritoryTo:"
        + (m_canBeGivenByTerritoryTo != null
            ? (m_canBeGivenByTerritoryTo.size() == 0 ? "empty" : m_canBeGivenByTerritoryTo.toString())
            : "null")
        + "  receivesAbilityWhenWith:"
        + (m_receivesAbilityWhenWith != null
            ? (m_receivesAbilityWhenWith.size() == 0 ? "empty" : m_receivesAbilityWhenWith.toString())
            : "null")
        + "  whenCombatDamaged:"
        + (m_whenCombatDamaged != null ? (m_whenCombatDamaged.size() == 0 ? "empty" : m_whenCombatDamaged.toString())
            : "null")
        + "  blockade:" + m_blockade + "  bombingMaxDieSides:" + m_bombingMaxDieSides + "  bombingBonus:"
        + m_bombingBonus + "  bombingTargets:" + m_bombingTargets + "  givesMovement:"
        + (m_givesMovement != null ? (m_givesMovement.size() == 0 ? "empty" : m_givesMovement.toString()) : "null")
        + "  repairsUnits:"
        + (m_repairsUnits != null ? (m_repairsUnits.isEmpty() ? "empty" : m_repairsUnits.toString()) : "null")
        + "  canScramble:" + m_canScramble + "  maxScrambleDistance:" + m_maxScrambleDistance + "  isAirBase:"
        + m_isAirBase + "  maxScrambleCount:" + m_maxScrambleCount + "  whenCapturedChangesInto:"
        + (m_whenCapturedChangesInto != null
            ? (m_whenCapturedChangesInto.size() == 0 ? "empty" : m_whenCapturedChangesInto.toString())
            : "null")
        + "  canIntercept:" + m_canIntercept + "  canEscort:" + m_canEscort + "  canAirBattle:" + m_canAirBattle
        + "  airDefense:" + m_airDefense + "  airAttack:" + m_airAttack + "  canNotMoveDuringCombatMove:"
        + m_canNotMoveDuringCombatMove + "  movementLimit:"
        + (m_movementLimit != null ? m_movementLimit.toString() : "null") + "  attackingLimit:"
        + (m_attackingLimit != null ? m_attackingLimit.toString() : "null") + "  placementLimit:"
        + (m_placementLimit != null ? m_placementLimit.toString() : "null")
        + "  tuv:" + m_tuv;
  }

  public String toStringShortAndOnlyImportantDifferences(final PlayerID player, final boolean useHtml,
      final boolean includeAttachedToName) {
    // displays everything in a very short form, in English rather than as xml stuff
    // shows all except for: m_constructionType, m_constructionsPerTerrPerTypePerTurn, m_maxConstructionsPerTypePerTerr,
    // m_canBeGivenByTerritoryTo, m_destroyedWhenCapturedBy, m_canBeCapturedOnEnteringBy
    final StringBuilder stats = new StringBuilder();
    final UnitType unitType = (UnitType) this.getAttachedTo();
    if (includeAttachedToName && unitType != null) {
      stats.append(unitType.getName()).append(":  ");
    }
    if (getIsAir()) {
      stats.append("Air unit, ");
    } else if (getIsSea()) {
      stats.append("Sea unit, ");
    } else {
      stats.append("Land unit, ");
    }
    final int attackRolls = getAttackRolls(player);
    final int defenseRolls = getDefenseRolls(player);
    if (getAttack(player) > 0) {
      stats.append(attackRolls > 1 ? (attackRolls + "x ") : "").append(getAttack(player)).append(" Attack, ");
    }
    if (getDefense(player) > 0) {
      stats.append(defenseRolls > 1 ? (defenseRolls + "x ") : "").append(getDefense(player)).append(" Defense, ");
    }
    if (getMovement(player) > 0) {
      stats.append(getMovement(player)).append(" Movement, ");
    }
    if (getHitPoints() > 1) {
      stats.append(getHitPoints()).append(" Hitpoints, ");
    }
    if (getCanProduceUnits() && getCanProduceXUnits() < 0) {
      stats.append("can Produce Units Up To Territory Value, ");
    } else if (getCanProduceUnits() && getCanProduceXUnits() > 0) {
      stats.append("can Produce ").append(getCanProduceXUnits()).append(" Units, ");
    }
    if (getCreatesUnitsList() != null && getCreatesUnitsList().size() > 0) {
      if (getCreatesUnitsList().size() > 4) {
        stats.append("Produces ").append(getCreatesUnitsList().totalValues()).append(" Units Each Turn, ");
      } else {
        stats.append("Produces ");
        for (final Entry<UnitType, Integer> entry : getCreatesUnitsList().entrySet()) {
          stats.append(entry.getValue()).append("x").append(entry.getKey().getName()).append(" ");
        }
        stats.append("Each Turn, ");
      }
    }
    if (getCreatesResourcesList() != null && getCreatesResourcesList().size() > 0) {
      if (getCreatesResourcesList().size() > 4) {
        stats.append("Produces ").append(getCreatesResourcesList().totalValues()).append(" Resources Each Turn, ");
      } else {
        stats.append("Produces ");
        for (final Entry<Resource, Integer> entry : getCreatesResourcesList().entrySet()) {
          stats.append(entry.getValue()).append("x").append(entry.getKey().getName()).append(" ");
        }
        stats.append("Each Turn, ");
      }
    }
    if (getFuelCost() != null && getFuelCost().size() > 0) {
      if (getFuelCost().size() > 4) {
        stats.append("Uses ").append(m_fuelCost.totalValues()).append(" Resources Each movement point, ");
      } else {
        stats.append("Uses ");
        for (final Entry<Resource, Integer> entry : getFuelCost().entrySet()) {
          stats.append(entry.getValue()).append("x").append(entry.getKey().getName()).append(" ");
        }
        stats.append("Each movement point, ");
      }
    }
    if ((getIsAAforCombatOnly() || getIsAAforBombingThisUnitOnly() || getIsAAforFlyOverOnly())
        && (getAttackAA(player) > 0 || getOffensiveAttackAA(player) > 0)) {
      if (getOffensiveAttackAA(player) > 0) {
        stats.append(getOffensiveAttackAA(player)).append("/").append(
            getOffensiveAttackAAmaxDieSides() != -1 ? getOffensiveAttackAAmaxDieSides() : getData().getDiceSides())
            .append(" att ");
      }
      if (getAttackAA(player) > 0) {
        stats.append(getAttackAA(player)).append("/")
            .append(getAttackAAmaxDieSides() != -1 ? getAttackAAmaxDieSides() : getData().getDiceSides())
            .append(" def ");
      }
      if (getIsAAforCombatOnly() && getIsAAforBombingThisUnitOnly() && getIsAAforFlyOverOnly()) {
        stats.append(getTypeAA()).append(", ");
      } else if (getIsAAforCombatOnly() && getIsAAforFlyOverOnly()
          && !Properties.getAATerritoryRestricted(getData())) {
        stats.append(getTypeAA()).append(" for Combat & Move Through, ");
      } else if (getIsAAforBombingThisUnitOnly() && getIsAAforFlyOverOnly()
          && !Properties.getAATerritoryRestricted(getData())) {
        stats.append(getTypeAA()).append(" for Raids & Move Through, ");
      } else if (getIsAAforCombatOnly()) {
        stats.append(getTypeAA()).append(" for Combat, ");
      } else if (getIsAAforBombingThisUnitOnly()) {
        stats.append(getTypeAA()).append(" for Raids, ");
      } else if (getIsAAforFlyOverOnly()) {
        stats.append(getTypeAA()).append(" for Move Through, ");
      }
      if (getMaxAAattacks() > -1) {
        stats.append(getMaxAAattacks()).append(" ").append(getTypeAA()).append(" Attacks, ");
      }
    }
    if (getIsRocket() && playerHasRockets(player)) {
      stats.append("can Rocket Attack, ");
      final int bombingBonus = getBombingBonus();
      if ((getBombingMaxDieSides() != -1 || bombingBonus != -1)
          && Properties.getUseBombingMaxDiceSidesAndBonus(getData())) {
        stats.append(bombingBonus != -1 ? bombingBonus + 1 : 1).append("-")
            .append(getBombingMaxDieSides() != -1 ? getBombingMaxDieSides() + (bombingBonus != -1 ? bombingBonus : 0)
                : getData().getDiceSides() + (bombingBonus != -1 ? bombingBonus : 0))
            .append(" Rocket Damage, ");
      } else {
        stats.append("1-").append(getData().getDiceSides()).append(" Rocket Damage, ");
      }
    }
    // line break
    if (useHtml) {
      stats.append("<br /> &nbsp;&nbsp;&nbsp;&nbsp; ");
    }
    if (getIsInfrastructure()) {
      stats.append("can be Captured, ");
    }
    if (getIsConstruction()) {
      stats.append("can be Placed Without Factory, ");
    }
    if ((getCanBeDamaged())
        && Properties.getDamageFromBombingDoneToUnitsInsteadOfTerritories(getData())) {
      stats.append("can be Damaged By Raids, ");
      if (getMaxOperationalDamage() > -1) {
        stats.append(getMaxOperationalDamage()).append(" Max Operational Damage, ");
      }
      if ((getCanProduceUnits()) && getCanProduceXUnits() < 0) {
        stats.append("Total Damage up to ").append(getMaxDamage() > -1 ? getMaxDamage() : 2)
            .append("x Territory Value, ");
      } else if (getMaxDamage() > -1) {
        stats.append(getMaxDamage()).append(" Max Total Damage, ");
      }
      if (getCanDieFromReachingMaxDamage()) {
        stats.append("will Die If Max Damage Reached, ");
      }
    } else if (getCanBeDamaged()) {
      stats.append("can be Attacked By Raids, ");
    }
    if (getIsAirBase() && Properties.getScramble_Rules_In_Effect(getData())) {
      stats.append("can Allow Scrambling, ");
    }
    if (getCanScramble() && Properties.getScramble_Rules_In_Effect(getData())) {
      stats.append("can Scramble ").append(getMaxScrambleDistance() > 0 ? getMaxScrambleDistance() : 1)
          .append(" Distance, ");
    }
    if (getArtillery()) {
      stats.append("can Give Attack Bonus To Other Units, ");
    } else {
      final List<UnitSupportAttachment> supports =
          Matches.getMatches(UnitSupportAttachment.get(unitType),
              Matches.unitSupportAttachmentCanBeUsedByPlayer(player));
      if (supports.size() > 0) {
        if (supports.size() > 2) {
          stats.append("can Modify Power Of Other Units, ");
        } else {
          for (final UnitSupportAttachment support : supports) {
            if (support.getUnitType() == null || support.getUnitType().isEmpty()) {
              continue;
            }
            stats.append("gives ").append(support.getBonus())
                .append(support.getStrength() && support.getRoll() ? " Power&Rolls"
                    : (support.getStrength() ? " Power" : " Rolls"))
                .append(" to ").append(support.getNumber())
                .append(support.getAllied() && support.getEnemy() ? " Allied&Enemy "
                    : (support.getAllied() ? " Allied " : " Enemy "))
                .append(support.getUnitType().size() > 4 ? "Units"
                    : MyFormatter.defaultNamedToTextList(support.getUnitType(), "/", false))
                .append(" when ")
                .append(support.getOffence() && support.getDefence() ? "Att/Def"
                    : (support.getOffence() ? "Attacking" : "Defending"))
                .append(", ");
          }
        }
      }
    }
    if (getArtillerySupportable()) {
      stats.append("can Receive Attack Bonus From Other Units, ");
    }
    if (getIsMarine() != 0) {
      stats.append(getIsMarine()).append(" Amphibious Attack Modifier, ");
    }
    if (getCanBlitz(player)) {
      stats.append("can Blitz, ");
    }
    if (!getReceivesAbilityWhenWith().isEmpty()) {
      if (getReceivesAbilityWhenWith().size() <= 2) {
        for (final String ability : getReceivesAbilityWhenWith()) {
          stats.append("receives ").append(ability.split(":")[0]).append(" when paired with ")
              .append(ability.split(":")[1]).append(", ");
        }
      } else {
        stats.append("receives Abilities When Paired with Other Units, ");
      }
    }
    if (getIsStrategicBomber()) {
      stats.append("can Perform Raids, ");
      final int bombingBonus = getBombingBonus();
      if ((getBombingMaxDieSides() != -1 || bombingBonus != -1)
          && Properties.getUseBombingMaxDiceSidesAndBonus(getData())) {
        stats.append(bombingBonus != -1 ? bombingBonus + 1 : 1).append("-")
            .append(getBombingMaxDieSides() != -1 ? getBombingMaxDieSides() + (bombingBonus != -1 ? bombingBonus : 0)
                : getData().getDiceSides() + (bombingBonus != -1 ? bombingBonus : 0))
            .append(" Raid Damage, ");
      } else {
        stats.append("1-").append(getData().getDiceSides()).append(" Raid Damage, ");
      }
    }
    final int airAttack = getAirAttack(player);
    final int airDefense = getAirDefense(player);
    if (airAttack > 0 && (getIsStrategicBomber() || getCanEscort() || getCanAirBattle())) {
      stats.append(attackRolls > 1 ? (attackRolls + "x ") : "").append(airAttack).append(" Air Attack, ");
    }
    if (airDefense > 0 && (getCanIntercept() || getCanAirBattle())) {
      stats.append(defenseRolls > 1 ? (defenseRolls + "x ") : "").append(airAttack).append(" Air Defense, ");
    }
    if (getIsSub()) {
      stats.append("is Stealth, ");
    }
    if (getIsDestroyer()) {
      stats.append("is Anti-Stealth, ");
    }
    if (getCanBombard(player) && getBombard() > 0) {
      stats.append(getBombard()).append(" Bombard, ");
    }
    if (getBlockade() > 0) {
      stats.append(getBlockade()).append(" Blockade Loss, ");
    }
    if (getIsSuicide()) {
      stats.append("Suicide/Munition Unit, ");
    }
    if (getIsAir() && (getIsKamikaze() || Properties.getKamikaze_Airplanes(getData()))) {
      stats.append("can use All Movement To Attack Target, ");
    }
    if (getIsInfantry() && playerHasMechInf(player)) {
      stats.append("can be Transported By Land, ");
    }
    if (getIsLandTransport() && playerHasMechInf(player)) {
      stats.append("is a Land Transport, ");
    }
    if (getIsAirTransportable() && playerHasParatroopers(player)) {
      stats.append("can be Transported By Air, ");
    }
    if (getIsAirTransport() && playerHasParatroopers(player)) {
      stats.append("is an Air Transport, ");
    }
    if (getIsCombatTransport() && getTransportCapacity() > 0) {
      stats.append("is a Combat Transport, ");
    } else if (getTransportCapacity() > 0 && getIsSea()) {
      stats.append("is a Sea Transport, ");
    }
    if (getTransportCost() > -1) {
      stats.append(getTransportCost()).append(" Transporting Cost, ");
    }
    if (getTransportCapacity() > 0 && getIsSea()) {
      stats.append(getTransportCapacity()).append(" Transporting Capacity, ");
    } else if (getTransportCapacity() > 0 && getIsAir() && playerHasParatroopers(player)) {
      stats.append(getTransportCapacity()).append(" Transporting Capacity, ");
    } else if (getTransportCapacity() > 0 && playerHasMechInf(player) && !getIsSea() && !getIsAir()) {
      stats.append(getTransportCapacity()).append(" Transporting Capacity, ");
    }
    if (getCarrierCost() > -1) {
      stats.append(getCarrierCost()).append(" Carrier Cost, ");
    }
    if (getCarrierCapacity() > 0) {
      stats.append(getCarrierCapacity()).append(" Carrier Capacity, ");
    }
    if (!getWhenCombatDamaged().isEmpty()) {
      stats.append("when hit this unit loses certain abilities, ");
    }
    // line break
    if (useHtml) {
      stats.append("<br /> &nbsp;&nbsp;&nbsp;&nbsp; ");
    }
    if (getMaxBuiltPerPlayer() > -1) {
      stats.append(getMaxBuiltPerPlayer()).append(" Max Built Allowed, ");
    }
    if (getRepairsUnits() != null && !getRepairsUnits().isEmpty()
        && Properties.getTwoHitPointUnitsRequireRepairFacilities(getData())
        && (Properties.getBattleshipsRepairAtBeginningOfRound(getData())
            || Properties.getBattleshipsRepairAtEndOfRound(getData()))) {
      if (getRepairsUnits().size() <= 4) {
        stats.append("can Repair: ")
            .append(MyFormatter.integerDefaultNamedMapToString(getRepairsUnits(), " ", "=", false)).append(", ");
      } else {
        stats.append("can Repair Some Units, ");
      }
    }
    if (getGivesMovement() != null && getGivesMovement().totalValues() > 0
        && Properties.getUnitsMayGiveBonusMovement(getData())) {
      if (getGivesMovement().size() <= 4) {
        stats.append("can Modify Unit Movement: ")
            .append(MyFormatter.integerDefaultNamedMapToString(getGivesMovement(), " ", "=", false)).append(", ");
      } else {
        stats.append("can Modify Unit Movement, ");
      }
    }
    if (getConsumesUnits() != null && getConsumesUnits().totalValues() == 1) {
      stats.append("unit is an Upgrade Of ").append(getConsumesUnits().keySet().iterator().next().getName())
          .append(", ");
    } else if (getConsumesUnits() != null && getConsumesUnits().totalValues() > 0) {
      if (getConsumesUnits().size() <= 4) {
        stats.append("unit Consumes On Placement: ")
            .append(MyFormatter.integerDefaultNamedMapToString(getConsumesUnits(), " ", "x", true)).append(", ");
      } else {
        stats.append("unit Consumes Other Units On Placement, ");
      }
    }
    if (getRequiresUnits() != null && getRequiresUnits().size() > 0
        && Properties.getUnitPlacementRestrictions(getData())) {
      final List<String> totalUnitsListed = new ArrayList<>();
      for (final String[] list : getRequiresUnits()) {
        totalUnitsListed.addAll(Arrays.asList(list));
      }
      if (totalUnitsListed.size() > 4) {
        stats.append("unit Requires Other Units Present To Be Placed, ");
      } else {
        stats.append("unit can only be Placed Where There Is: ");
        final Iterator<String[]> requiredIter = getRequiresUnits().iterator();
        while (requiredIter.hasNext()) {
          final String[] required = requiredIter.next();
          if (required.length == 1) {
            stats.append(required[0]);
          } else {
            stats.append(Arrays.toString(required));
          }
          if (requiredIter.hasNext()) {
            stats.append(" Or ");
          }
        }
        stats.append(", ");
      }
    }
    if (getRequiresUnitsToMove() != null && !getRequiresUnitsToMove().isEmpty()) {
      final List<String> totalUnitsListed = new ArrayList<>();
      for (final String[] list : getRequiresUnitsToMove()) {
        totalUnitsListed.addAll(Arrays.asList(list));
      }
      if (totalUnitsListed.size() > 4) {
        stats.append("unit Requires Other Units Present To Be Moved, ");
      } else {
        stats.append("unit can only be Moved Where There Is: ");
        final Iterator<String[]> requiredIter = getRequiresUnitsToMove().iterator();
        while (requiredIter.hasNext()) {
          final String[] required = requiredIter.next();
          if (required.length == 1) {
            stats.append(required[0]);
          } else {
            stats.append(Arrays.toString(required));
          }
          if (requiredIter.hasNext()) {
            stats.append(" Or ");
          }
        }
        stats.append(", ");
      }
    }
    if (getUnitPlacementRestrictions() != null
        && Properties.getUnitPlacementRestrictions(getData())) {
      stats.append("has Placement Restrictions, ");
    }
    if (getCanOnlyBePlacedInTerritoryValuedAtX() > 0
        && Properties.getUnitPlacementRestrictions(getData())) {
      stats.append("must be Placed In Territory Valued >=").append(getCanOnlyBePlacedInTerritoryValuedAtX())
          .append(", ");
    }
    if (getCanNotMoveDuringCombatMove()) {
      stats.append("cannot Combat Move, ");
    }
    if (getMovementLimit() != null) {
      if (getMovementLimit().getFirst() == Integer.MAX_VALUE
          && (getIsAAforBombingThisUnitOnly() || getIsAAforCombatOnly())
          && !(Properties.getWW2V2(getData())
              || Properties.getWW2V3(getData())
              || Properties.getMultipleAAPerTerritory(getData()))) {
        stats.append("max of 1 ").append(getMovementLimit().getSecond()).append(" moving per territory, ");
      } else if (getMovementLimit().getFirst() < 10000) {
        stats.append("max of ").append(getMovementLimit().getFirst()).append(" ").append(getMovementLimit().getSecond())
            .append(" moving per territory, ");
      }
    }
    if (getAttackingLimit() != null) {
      if (getAttackingLimit().getFirst() == Integer.MAX_VALUE
          && (getIsAAforBombingThisUnitOnly() || getIsAAforCombatOnly())
          && !(Properties.getWW2V2(getData())
              || Properties.getWW2V3(getData())
              || Properties.getMultipleAAPerTerritory(getData()))) {
        stats.append("max of 1 ").append(getAttackingLimit().getSecond()).append(" attacking per territory, ");
      } else if (getAttackingLimit().getFirst() < 10000) {
        stats.append("max of ").append(getAttackingLimit().getFirst()).append(" ")
            .append(getAttackingLimit().getSecond()).append(" attacking per territory, ");
      }
    }
    if (getPlacementLimit() != null) {
      if (getPlacementLimit().getFirst() == Integer.MAX_VALUE
          && (getIsAAforBombingThisUnitOnly() || getIsAAforCombatOnly())
          && !(Properties.getWW2V2(getData())
              || Properties.getWW2V3(getData())
              || Properties.getMultipleAAPerTerritory(getData()))) {
        stats.append("max of 1 ").append(getPlacementLimit().getSecond()).append(" placed per territory, ");
      } else if (getPlacementLimit().getFirst() < 10000) {
        stats.append("max of ").append(getPlacementLimit().getFirst()).append(" ")
            .append(getPlacementLimit().getSecond()).append(" placed per territory, ");
      }
    }
    if (stats.indexOf(", ") > -1) {
      stats.delete(stats.lastIndexOf(", "), stats.length() - 1);
    }
    return stats.toString();
  }

  /**
   * @deprecated does nothing, kept to avoid breaking maps, do not remove.
   */
  @Deprecated
  @GameProperty(xmlProperty = true, gameProperty = false, adds = false)
  public void setIsParatroop(final String s) {}

  /**
   * @deprecated does nothing, used to keep compatibility with older xml files, do not remove.
   */
  @Deprecated
  @GameProperty(xmlProperty = true, gameProperty = false, adds = false)
  public void setIsMechanized(final String s) {}
}
