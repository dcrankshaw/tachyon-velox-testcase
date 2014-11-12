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
import scala.util.Random

object TachyonKVExercise {

  def main(args: Array[String]) {
    // tachyonlocation = "tachyon://ec2-1.1.1.1.compute-1.amazonaws.com:19998/test-store
    println(args.mkString(", "))
    val tachyonLocation: String = args(0)
    val sleepTime: Long = args(1).toLong
    val numEntries: Int = args(2).toInt
    // val testStore = "test-store"
    val numPartitions = 3
    // val numEntries = 1000000
    val rand = new Random
    val bigArray = new Array[Double](100)
    var i = 0
    while (i < bigArray.size) {
      bigArray(i) = rand.nextDouble()
      i += 1
    }

    val kryo = KryoThreadLocal.kryoTL.get
    val buffer = ByteBuffer.allocate(bigArray.size*8*8*2)
    val serArray = kryo.serialize(bigArray, buffer).array

    val kvstore = ClientStore.createStore(new TachyonURI(tachyonLocation))
    println("Created kv store")
    val testData = (0 until numEntries).map {i =>
      if (i % 1000 == 0) {
        println(s"processing entry $i")
      }
      (TachyonUtils.long2ByteArr(i), serArray)
    }
    val sortedObs = TreeMap(testData.toArray:_*)(ByteOrdering)
    // write to KV store
    println("Writing to tachyon")
    writeMapToTachyon(sortedObs, kvstore, numPartitions)

    println("Sleeping")
    Thread.sleep(sleepTime)
    val kvstoreGet = ClientStore.getStore(new TachyonURI(tachyonLocation))

    // Try getting from KV store
    println(s"Reading from Tachyon, time is: ${System.currentTimeMillis}")
    val results = (0 until 2).map { i =>
      val result = kvstoreGet.get(TachyonUtils.long2ByteArr(i))
      val resultArray = kryo.deserialize(ByteBuffer.wrap(result)).asInstanceOf[Array[Double]]
      print(s"result length: ${result.length}")
      s"($i -> ${resultArray.mkString(",")})"
      // (i, result)
    }

    println(s"Array should be: ${bigArray.mkString(",")}")
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
      if (i % 1000 == 0) {
        println(s"Writing key $i")

      }
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
