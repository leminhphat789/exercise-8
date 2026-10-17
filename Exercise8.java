import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/** Bài thực hành Exercise 8, chạy độc lập với Java 8 trở lên và dùng một luồng. */
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

    // Eager: tạo sẵn 52 lá bài khi lớp được khởi tạo ở lần sử dụng đầu tiên.
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

    // Lazy: Map ban đầu rỗng, chỉ tạo lá bài khi được yêu cầu.
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
                    // Lưu đối tượng trả về vào biến volatile; thao tác này thêm chi phí cho cả hai cách.
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
        // Chưa sử dụng cache của cả hai lớp trước các phép đo này.
        long first = obtainAll(lazyFirst, 1);
        long second = obtainAll(!lazyFirst, 1);
        System.out.println("Java: " + System.getProperty("java.version"));
        System.out.println("Order: " + (lazyFirst ? "lazy, eager" : "eager, lazy"));
        System.out.println("First 52, eager: " + (lazyFirst ? second : first) + " ms");
        System.out.println("First 52, lazy:  " + (lazyFirst ? first : second) + " ms");
        verify();
        System.out.println("PASS: 52 distinct lazy cards; both caches reuse instances.");
        // Đo bổ sung: chỉ lấy lại lá bài trong cache, không tạo mới qua mỗi lượt.
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
