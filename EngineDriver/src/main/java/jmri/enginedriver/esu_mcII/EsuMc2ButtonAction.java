package jmri.enginedriver.esu_mcII;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import jmri.enginedriver.throttle;

public enum EsuMc2ButtonAction {
    NO_ACTION("(no function)"),
    ALL_STOP("All Stop"),
    STOP("Stop"),
    DIRECTION_FORWARD("Forward"),
    DIRECTION_REVERSE("Reverse"),
    DIRECTION_TOGGLE("Forward/Reverse Toggle"),
    SPEED_INCREASE("Increase Speed"),
    SPEED_DECREASE("Decrease Speed"),
    NEXT_THROTTLE("Next Throttle"),
    FN00("Function 00/Light", 0),
    FN01("Function 01/Bell", 1),
    FN02("Function 02/Horn", 2),
    FN03("Function 03", 3),
    FN04("Function 04", 4),
    FN05("Function 05", 5),
    FN06("Function 06", 6),
    FN07("Function 07", 7),
    FN08("Function 08", 8),
    FN09("Function 09", 9),
    FN10("Function 10", 10),
    FN11("Function 11", 11),
    FN12("Function 12", 12),
    FN13("Function 13", 13),
    FN14("Function 14", 14),
    FN15("Function 15", 15),
    FN16("Function 16", 16),
    FN17("Function 17", 17),
    FN18("Function 18", 18),
    FN19("Function 19", 19),
    FN20("Function 20", 20),
    FN21("Function 21", 21),
    FN22("Function 22", 22),
    FN23("Function 23", 23),
    FN24("Function 24", 24),
    FN25("Function 25", 25),
    FN26("Function 26", 26),
    FN27("Function 27", 27),
    FN28("Function 28", 28),
    FN29("Function 29", 29),
    FN30("Function 30", 30),
    FN31("Function 31", 31);

    private final String action;
    private final int function;

    private static final Map<String, EsuMc2ButtonAction> ENUM_MAP;

    EsuMc2ButtonAction(String action) {
        this(action, -1);
    }

    EsuMc2ButtonAction(String action, int function) {
        this.action = action;
        this.function = function;
    }

    private String getAction() {
        return this.action;
    }

    public int getFunction() {
        return this.function;
    }

    // Build immutable map of String name to enum pairs

    static {
        Map<String, EsuMc2ButtonAction> map = new ConcurrentHashMap<>();
        for (EsuMc2ButtonAction action : EsuMc2ButtonAction.values()) {
            map.put(action.getAction(), action);
        }
        ENUM_MAP = Collections.unmodifiableMap(map);
    }

    public static EsuMc2ButtonAction getAction(String action) {
        return ENUM_MAP.get(action);
    }
}
