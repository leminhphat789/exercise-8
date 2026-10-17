# Exercise 8 - Lazy Card Flyweights

Bài tập so sánh cách tạo sẵn 52 lá bài (eager) và chỉ tạo khi cần dùng (lazy).

Đề bài: [Exercise 8 - DesignBook](https://github.com/prmr/DesignBook/blob/master/exercises/e-chapter4.md).

Đề có nhắc Exercise 9, nhưng bài về flyweight là Exercise 7 nên bài này làm tiếp từ Exercise 7.

## Cách chạy

Cần cài JDK 8 trở lên. Mở terminal tại thư mục chứa file rồi chạy:

```sh
javac -encoding UTF-8 Exercise8.java
java Exercise8
java Exercise8 lazy-first
```

Lệnh cuối chạy lại chương trình với lazy được đo trước.

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

0 ms không có nghĩa là không mất thời gian. Lấy 52 lá bài quá nhanh nên khó thấy chênh lệch bằng đồng hồ mili giây. Độ phân giải của currentTimeMillis() còn tùy hệ điều hành, theo [tài liệu Oracle](https://docs.oracle.com/javase/8/docs/api/java/lang/System.html#currentTimeMillis--). Kết quả có thể khác khi chạy lại.

## Kết luận

Nếu dùng đủ 52 lá thì cả hai cách đều tạo 52 đối tượng. Lazy chỉ dời việc tạo sang lúc cần, không giảm số đối tượng cuối cùng.

Nếu chỉ dùng vài lá thì lazy có lợi vì không phải tạo những lá còn lại. Nếu luôn dùng cả bộ bài thì eager đơn giản và phù hợp.

Các số đo trên chưa đủ để kết luận cách nào nhanh hơn.
