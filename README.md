# Exercise 8
## Cách chạy

Cần cài JDK. Mở terminal tại thư mục chứa file rồi chạy:

```sh
javac -encoding UTF-8 Exercise8.java
java Exercise8
java Exercise8 lazy-first
```

## Cách làm

EagerCard tạo sẵn 52 lá bài trong khối static và lưu vào Map.

Card dùng lazy: ban đầu Map rỗng. Khi gọi get(), nếu lá bài đã có thì trả về đối tượng cũ. Nếu chưa có thì tạo mới, lưu vào Map rồi trả về.

```java
public static Card get(Rank rank, Suit suit) {
    int id = key(rank, suit);
    Card card = CACHE.get(id);
    if (card == null) {
        card = new Card(rank, suit);
        CACHE.put(id, card);
    }
    return card;
}
```

Khóa được tính bằng suit.ordinal() * 13 + rank.ordinal(), nên mỗi cặp hạng và chất có một khóa riêng. Constructor private để lấy lá bài qua get(). Các thuộc tính final giúp lá bài không bị thay đổi.

Hai lần lấy cùng một lá sẽ nhận cùng một đối tượng. Bản này chạy một luồng; nếu dùng nhiều luồng thì cần đồng bộ thao tác lấy và tạo lá bài.

## Kết quả chạy

Dùng System.currentTimeMillis() đo từ trước lần gọi get() đầu tiên đến khi lấy đủ 52 lá, có tính cả thời gian khởi tạo cache.

Kết quả ghi lại trên Java 21.0.12.1:

- Chạy eager trước: eager 0 ms, lazy 0 ms.
- Chạy lazy trước trong JVM mới: eager 0 ms, lazy 4 ms.

Sau khi cache đã đầy, chương trình làm nóng mỗi cách 20.000 lượt rồi đo thêm 4 vòng. Mỗi vòng lấy đủ 52 lá 200.000 lần và đổi thứ tự đo giữa các vòng:

- Lần chạy eager trước ban đầu: eager 31-33 ms, lazy 31-34 ms.
- Lần chạy lazy trước ban đầu: eager 38-45 ms, lazy 36-43 ms.

Phần đo thêm chỉ lấy lại lá bài đã có, không tạo mới mỗi lượt. Vòng lặp có ghi kết quả vào biến volatile nên thời gian cũng gồm chi phí này.

Kiểm tra tính duy nhất và tái sử dụng đối tượng đều PASS. Kết quả in từ chương trình được lưu trong [results.txt](results.txt) và [results-lazy-first.txt](results-lazy-first.txt).

