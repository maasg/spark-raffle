package pipelines.example

import pipelines.streamlets.StreamletShape
import pipelines.streamlets.avro._
import pipelines.spark.{ SparkStreamlet, SparkStreamletLogic }
import org.apache.spark.sql.Dataset
import org.apache.spark.sql.functions._

import pipelines.spark.sql.SQLImplicits._
import org.apache.spark.sql.streaming.{ GroupStateTimeout, OutputMode }

class RaffleState extends SparkStreamlet {

  val in = AvroInlet[Participant]("in")
  val out = AvroOutlet[Votes]("out", _.number.toString)
  val shape = StreamletShape(in, out)
  val MagicNumber = 87

  import org.apache.spark.sql.streaming.GroupState
  def flatMappingFunction(key: Int, values: Iterator[Participant], state: GroupState[Votes]): Iterator[Votes] = {
    if (state.hasTimedOut) {
      // when the state has a timeout, the values are empty
      // this validation is only to illustrate that point
      assert(values.isEmpty, "When the state has a timeout, the values are empty")
      // evict the timed-out state
      state.remove()
      Iterator.empty
    } else {
      // get current state or create a new one if there's no previous state
      val currentState = state.getOption.getOrElse {
        val status = if (key == MagicNumber) "Winner !!!" else "--"
        Votes(key, values.next.name, status)
      }
      state.update(currentState)
      List(currentState).toIterator
    }
  }

  override def createLogic() = new SparkStreamletLogic {
    override def buildStreamingQueries = {
      val dataset = readStream(in)
      val outStream = process(dataset)
      writeStream(outStream, out, OutputMode.Append).toQueryExecution
    }

    private def process(inDataset: Dataset[Participant]): Dataset[Votes] = {
      val query = inDataset.groupByKey(participant ⇒ participant.guess)
        .flatMapGroupsWithState(OutputMode.Update, GroupStateTimeout.NoTimeout())(flatMappingFunction)
      query.as[Votes]
    }
  }

}
