import org.joda.time.{DateTime, DateTimeZone}
import org.scalameter.api._


object RangeBenchmark
  extends Bench.LocalTime {
  val sizes = Gen.range("size")(300000, 1500000, 300000)
  val ranges = for {
    size <- sizes
  } yield 0 until size


  performance of "Range" in {
    print(DateTime.now(DateTimeZone.UTC).getMillis() + "\n")
    measure method "map" in {
      using(ranges) in {
        r => r.map(_ + 1)

      }
    }
    print(DateTime.now(DateTimeZone.UTC).getMillis())
  }
}
