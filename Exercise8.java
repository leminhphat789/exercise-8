import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/** Standalone Exercise 8 solution. Java 8+, single-threaded experiment. */
public class Exercise8 {
    public enum Rank {
        ACE, TWO, THREE, FOUR, FIVE, SIX, SEVEN,
        EIGHT, NINE, TEN, JACK, QUEEN, KING
    }
    public enum Suit { CLUBS, DIAMONDS, HEARTS, SPADES }
    private static final Rank[] RANKS = Rank.values();
    private static final Suit[] SUITS = Suit.values();
    private static volatile Object sink;

    private static int key(Rank rank, Suit suit) {
        Objects.requireNonNull(rank);
        Objects.requireNonNull(suit);
        return suit.ordinal() * RANKS.length + rank.ordinal();
    }

    // Eager: initialization occurs on the first active use of this class.
    public static final class EagerCard {
        private final Rank rank;
        private final Suit suit;
        private static final Map<Integer, EagerCard> CACHE = new HashMap<>();
        static {
            for (Suit suit : SUITS) {
                for (Rank rank : RANKS) {
                    CACHE.put(key(rank, suit), new EagerCard(rank, suit));
                }
            }
        }
        private EagerCard(Rank rank, Suit suit) {
            this.rank = rank;
            this.suit = suit;
        }
        public static EagerCard get(Rank rank, Suit suit) {
            return CACHE.get(key(rank, suit));
        }
        public Rank rank() { return rank; }
        public Suit suit() { return suit; }
    }

    // Lazy: the map starts empty; each distinct card is created on demand.
    public static final class Card implements Comparable<Card> {
        private final Rank rank;
        private final Suit suit;
        private static final Map<Integer, Card> CACHE = new HashMap<>();
        private Card(Rank rank, Suit suit) {
            this.rank = rank;
            this.suit = suit;
        }
        public static Card get(Rank rank, Suit suit) {
            int id = key(rank, suit);
            Card card = CACHE.get(id);
            if (card == null) {
                card = new Card(rank, suit);
                CACHE.put(id, card);
            }
            return card;
        }
        public Rank rank() { return rank; }
        public Suit suit() { return suit; }
        @Override public int compareTo(Card other) {
            return rank.compareTo(other.rank);
        }
        public static Comparator<Card> createByRankComparator() {
            return Comparator.comparing(Card::rank);
        }
        @Override public String toString() { return rank + " of " + suit; }
    }

    private static long obtainAll(boolean lazy, int repetitions) {
        long start = System.currentTimeMillis();
        for (int i = 0; i < repetitions; i++) {
            for (Suit suit : SUITS) {
                for (Rank rank : RANKS) {
                    // Make returned objects observable; adds overhead to BOTH cases.
                    if (lazy) sink = Card.get(rank, suit);
                    else sink = EagerCard.get(rank, suit);
                }
            }
        }
        return System.currentTimeMillis() - start;
    }

    private static void verify() {
        if (Card.CACHE.size() != 52 || EagerCard.CACHE.size() != 52) {
            throw new AssertionError("Expected exactly 52 cached cards");
        }
        java.util.Set<Card> identities = java.util.Collections.newSetFromMap(
            new java.util.IdentityHashMap<Card, Boolean>());
        for (Suit suit : SUITS) {
            for (Rank rank : RANKS) {
                Card card = Card.get(rank, suit);
                if (card != Card.get(rank, suit)
                        || card.rank() != rank || card.suit() != suit
                        || EagerCard.get(rank, suit) != EagerCard.get(rank, suit)) {
                    throw new AssertionError("Invalid flyweight");
                }
                identities.add(card);
            }
        }
        if (identities.size() != 52) throw new AssertionError("Distinct cards aliased");
    }

    public static void main(String[] args) {
        boolean lazyFirst = args.length > 0 && args[0].equals("lazy-first");
        // Do not touch either cache before these measurements.
        long first = obtainAll(lazyFirst, 1);
        long second = obtainAll(!lazyFirst, 1);
        System.out.println("Java: " + System.getProperty("java.version"));
        System.out.println("Order: " + (lazyFirst ? "lazy, eager" : "eager, lazy"));
        System.out.println("First 52, eager: " + (lazyFirst ? second : first) + " ms");
        System.out.println("First 52, lazy:  " + (lazyFirst ? first : second) + " ms");
        verify();
        System.out.println("PASS: 52 distinct lazy cards; both caches reuse instances.");
        // Supplemental measurement: cached lookups ONLY, not repeated creation.
        obtainAll(false, 20000);
        obtainAll(true, 20000);
        int repetitions = 200000;
        for (int round = 0; round < 4; round++) {
            boolean lazyRunsFirst = round % 2 != 0;
            long a = obtainAll(lazyRunsFirst, repetitions);
            long b = obtainAll(!lazyRunsFirst, repetitions);
            System.out.println("Cached round " + (round + 1) + ", " + repetitions
                + " x 52: eager=" + (lazyRunsFirst ? b : a)
                + " ms, lazy=" + (lazyRunsFirst ? a : b) + " ms");
        }
    }
}
