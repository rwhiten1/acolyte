package acolyte.reactivemongo

import java.nio.ByteOrder

import org.jboss.netty.buffer.ChannelBuffers

import scala.util.Try

import reactivemongo.bson.BSONDocument
import reactivemongo.core.protocol.{
  MessageHeader,
  Reply,
  Response,
  ResponseInfo
}

/* MongoDB companion */
object MongoDB {

  /**
   * Build a Mongo response.
   *
   * @param channelId Unique ID of channel
   * @param docs BSON documents
   */
  def mkResponse(channelId: Int, docs: BSONDocument*): Try[Response] = Try {
    val body = new reactivemongo.bson.buffer.ArrayBSONBuffer()

    docs foreach { d =>
      BSONDocument.write(d, body)
    }

    val len = 36 /* header size */ + body.index
    val buf = ChannelBuffers.buffer(ByteOrder.LITTLE_ENDIAN, len)

    buf.writeInt(len)
    buf.writeInt(System identityHashCode docs) // fake response ID
    buf.writeInt(System identityHashCode buf) // fake request ID
    buf.writeInt(4 /* OP_REPLY */ ) // opCode
    buf.writeInt(4 /* ignore */ ) // OR: 1 = QueryFailure
    buf.writeLong(0) // cursor ID
    buf.writeInt(0) // cursor starting from
    buf.writeInt(docs.size) // number of document
    buf.writeBytes(body.array)

    val in = ChannelBuffers.unmodifiableBuffer(buf)
    Response(MessageHeader(in), Reply(in), in, ResponseInfo(channelId))
  }
}