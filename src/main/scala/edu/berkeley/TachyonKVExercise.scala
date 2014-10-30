package edu.berkeley

import tachyon.r.sorted.ClientStore
import tachyon.TachyonURI
import java.nio.ByteBuffer
import scala.util._
import java.io.IOException
import java.nio.ByteBuffer
import scala.collection.immutable.HashMap
import java.nio.ByteBuffer
import java.io.ByteArrayOutputStream
import scala.collection.immutable.TreeMap
import tachyon.r.sorted.{Utils => TUtils}

object TachyonKVExercise {

  def main(args: Array[String]) {
    // tachyonlocation = "tachyon://ec2-1.1.1.1.compute-1.amazonaws.com:19998/test-store
    val tachyonLocation: String = args(1)
    // val testStore = "test-store"
    val numPartitions = 3
    val numEntries = 50

    val kvstore = ClientStore.createStore(new TachyonURI(tachyonLocation))
    val testData = (0 until numEntries).map {i =>
      (TachyonUtils.long2ByteArr(i), TachyonUtils.long2ByteArr(i*7))
    }
    val sortedObs = TreeMap(testData.toArray:_*)(ByteOrdering)
    // write to KV store
    writeMapToTachyon(sortedObs, kvstore, numPartitions)

    Thread.sleep(10000L)

    // Try getting from KV store
    val results = (0 until numEntries).map { i =>
      val result = TachyonUtils.byteArr2Long(kvstore.get(TachyonUtils.long2ByteArr(i)))
      s"($i -> $result)"
      // (i, result)
    }

    println(s"Results: ${results.mkString(", ")}")
  }

  def writeMapToTachyon(
      map: TreeMap[Array[Byte], Array[Byte]],
      store: ClientStore,
      splits: Int) {
    val partition = 0
    for (pa <- 0 to splits) {
      store.createPartition(partition + pa)
    }
    val splitPoint: Int = map.size / splits
    var i = 0
    val keys = map.keys.toList
    while (i < map.size) {
      val k: Array[Byte] = keys(i)
      val v: Array[Byte] = map(k)
      val p: Int = partition + (i / splitPoint)
      store.put(p, k, v)
      i += 1
    }

    for (pa <- 0 to splits) {
      store.closePartition(partition + pa)
    }
  }


}

object ByteOrdering extends Ordering[Array[Byte]] {
  def compare(a: Array[Byte], b: Array[Byte]) = TUtils.compare(a, b)
}

object TachyonUtils {

  def long2ByteArr(id: Long): Array[Byte] = {
    // val key = ByteBuffer.allocate(8)
    // key.putLong(id).array()

    val buffer = ByteBuffer.allocate(12)
    val kryo = KryoThreadLocal.kryoTL.get
    val result = kryo.serialize(id, buffer).array
    result
  }

  def byteArr2Long(arr: Array[Byte]): Long = {
    val rawBytes = ByteBuffer.wrap(arr)
    val kryo = KryoThreadLocal.kryoTL.get
    val result = kryo.deserialize(rawBytes).asInstanceOf[Long]
    result

  }

  // could make this a z-curve key instead
  def twoDimensionKey(key1: Long, key2: Long): Array[Byte] = {
    val key = ByteBuffer.allocate(16)
    key.putLong(key1)
    key.putLong(key2)
    key.array()
  }

  def getStore(tachyon: String, kvloc: String): Try[ClientStore] = {
    val url = s"$tachyon/$kvloc"
    Try(ClientStore.getStore(new TachyonURI(url))) match {
      case f: Failure[ClientStore] => f
      case Success(u) => if (u == null) {
        Failure(new RuntimeException(s"Tachyon store $url not found"))
      } else {
        Success(u)
      }
    }
  }
}
