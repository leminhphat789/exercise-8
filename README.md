# Exercise 8 — Lazy Card Flyweights

Bài thực hành Java so sánh hai cách tạo đối tượng dùng chung (Flyweight): eager initialization và lazy initialization.

## Đề bài và cách hiểu

Nguồn: [DesignBook, Chapter 4, Exercise 8](https://github.com/prmr/DesignBook/blob/master/exercises/e-chapter4.md), Martin P. Robillard.

Đề yêu cầu chuyển sang tạo card flyweight theo nhu cầu, dùng `System.currentTimeMillis()` đo thời gian lấy đủ 52 lá bài và rút ra kết luận. Tham chiếu “Exercise 9” có vẻ là lỗi đánh số: Exercise 7 mới yêu cầu flyweight khởi tạo trước bằng Map; Exercise 9 nói về Singleton. Lời giải này tiếp nối yêu cầu của Exercise 7 và được viết thành chương trình độc lập.

## Chạy chương trình

Yêu cầu JDK 8 trở lên. Tại thư mục chứa mã nguồn:

```sh
javac -encoding UTF-8 Exercise8.java
java Exercise8
java Exercise8 lazy-first
```

Mỗi lệnh `java` tạo một JVM mới, giúp đo lần sử dụng đầu tiên với cache chưa được khởi tạo. Lệnh thứ hai đảo thứ tự eager/lazy. Mã đã được biên dịch với `javac --release 8` và chạy trên Java 21.0.12.1.

## Giải pháp

- `EagerCard`: khối static tạo trước đủ 52 đối tượng khi lớp được khởi tạo. Sau đó `get()` chỉ tra cứu Map.
- `Card`: Map ban đầu rỗng. `get()` tra cứu, chỉ tạo và lưu đối tượng nếu chưa có.
- Khóa `suit.ordinal() * 13 + rank.ordinal()` ánh xạ duy nhất mỗi cặp hạng–chất vào một số từ 0 đến 51.
- Constructor private ngăn việc tạo lá bài ngoài factory; thuộc tính final giữ trạng thái bất biến.
- Hai lần gọi với cùng hạng và chất trả về cùng một đối tượng.

Phần cốt lõi của lazy initialization:

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

Đây là thực hành một luồng. Nếu dùng nhiều luồng, phải đồng bộ hóa toàn bộ thao tác kiểm tra–tạo–lưu để bảo đảm tính duy nhất.

## Phương pháp đo

Đặt thời điểm bắt đầu trước lần gọi factory đầu tiên, rồi duyệt 4 chất × 13 hạng. Cả hai cách đều tính chi phí khởi tạo lớp/cache trong lần đo đầu; không tạo trước cache eager ngoài vùng đo.

Sau đó chương trình kiểm tra cache có 52 phần tử, các cặp hạng–chất khác nhau của lazy không dùng nhầm cùng đối tượng, và cả hai factory tái sử dụng đối tượng.

Phép đo bổ sung làm nóng mỗi cách qua 20.000 lượt, rồi đo bốn vòng, mỗi vòng 200.000 lượt lấy đủ 52 lá; đảo thứ tự giữa các vòng. Phần này **chỉ đo tra cứu cache đã đầy**, không đo tạo lại đối tượng. Biến volatile giữ kết quả quan sát được nhưng cũng thêm chi phí cho cả hai cách.

## Kết quả thực tế

| Thứ tự trong JVM mới | Eager: lần đầu lấy 52 lá | Lazy: lần đầu lấy 52 lá |
| --- | ---: | ---: |
| Eager trước | 0 ms | 0 ms |
| Lazy trước | 0 ms | 4 ms |

| Tiến trình | Eager: 200.000 × 52 lượt, khoảng qua 4 vòng | Lazy: cùng số lượt |
| --- | ---: | ---: |
| Eager chạy trước ban đầu | 31–33 ms | 31–34 ms |
| Lazy chạy trước ban đầu | 38–45 ms | 36–43 ms |

Kiểm tra tính đúng đắn đều PASS. Xem log gốc trong [results.txt](results.txt) và [results-lazy-first.txt](results-lazy-first.txt).

Số đo 0 ms không có nghĩa là không mất thời gian. Theo [Oracle](https://docs.oracle.com/javase/8/docs/api/java/lang/System.html#currentTimeMillis--), đơn vị trả về là mili giây nhưng độ phân giải thực tế phụ thuộc hệ điều hành. Kết quả còn chịu ảnh hưởng của việc nạp lớp, JIT, lịch chạy hệ điều hành và chi phí vòng lặp. Đây là phép đo minh họa cho bài tập, chưa đủ để kết luận cách nào nhanh hơn.

## Kết luận

Khi cần đủ 52 lá, cả hai cách đều tạo và lưu 52 đối tượng, nên lazy không tiết kiệm số đối tượng ở trạng thái cuối. Eager trả chi phí tạo ngay khi lớp khởi tạo; lazy dời chi phí sang lần yêu cầu đầu tiên của từng lá.

Lazy hữu ích khi chỉ dùng một phần bộ bài: các lá chưa được yêu cầu chưa cần tạo. Với bộ bài nhỏ và thường dùng đầy đủ, eager đơn giản và phù hợp. Chọn chiến lược dựa trên nhu cầu sử dụng và thời điểm trả chi phí khởi tạo, không dựa vào chênh lệch vài mili giây trong phép đo nhỏ này.
