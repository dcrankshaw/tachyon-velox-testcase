##Minimal example that reproduces the Tachyon KV store issue.

The issue only seems to occur when running Tachyon on top of S3, when the partition being accessed is not in memory.

The error occurs when using the [dcrankshaw/velox-build](https://github.com/dcrankshaw/tachyon/tree/velox-build)
branch of Tachyon.

Here is the stack trace from Tachyon master that creates the empty partition. This then causes the Tachyon client
to try to create an `InetAddress` with port=-1, triggering an exception.
```
2014-10-30 22:20:08,636 INFO  MASTER_LOGGER (StoresInfo.java:getPartition) - MasterPartition empty location: blockinfo([ClientBlockInfo(blockId:18253611008, offset:0, length:133332, locations:[NetAddress(mHost:localhost, mPort:-1, mSecondaryPort:-1)])]); SortedStorePartitionInfoSortedStorePartitionInfo(storeType:null, storeId:11, partitionIndex:2, dataFileId:16, indexFileId:17, startKey:09 D4 DF 02 00 00 00 00 00 00 00 00, endKey:09 7C 00 00 00 00 00 00 00 00 00 00, location:NetAddress(mHost:localhost, mPort:-1, mSecondaryPort:-1)) STACKTRACE: java.lang.Exception: Stack Trace
	at tachyon.r.sorted.master.StoresInfo.getPartition(StoresInfo.java:100)
	at tachyon.r.sorted.master.StoresInfo.process(StoresInfo.java:181)
	at tachyon.master.MasterServiceHandler.x_process(MasterServiceHandler.java:386)
	at tachyon.thrift.MasterService$Processor$x_process.getResult(MasterService.java:4093)
	at tachyon.thrift.MasterService$Processor$x_process.getResult(MasterService.java:4077)
	at tachyon.org.apache.thrift.ProcessFunction.process(ProcessFunction.java:39)
	at tachyon.org.apache.thrift.TBaseProcessor.process(TBaseProcessor.java:39)
	at tachyon.org.apache.thrift.server.AbstractNonblockingServer$FrameBuffer.invoke(AbstractNonblockingServer.java:516)
	at tachyon.org.apache.thrift.server.Invocation.run(Invocation.java:18)
	at java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1145)
	at java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:615)
	at java.lang.Thread.run(Thread.java:745)

```

And here is the stack trace from the client program:
```
Exception in thread "main" java.lang.IllegalArgumentException: port out of range:-1
        at java.net.InetSocketAddress.checkPort(InetSocketAddress.java:143)
        at java.net.InetSocketAddress.<init>(InetSocketAddress.java:224)
        at tachyon.r.sorted.ClientStore.get(ClientStore.java:88)
        at edu.berkeley.TachyonKVExercise$$anonfun$2.apply(TachyonKVExercise.scala:60)
        at edu.berkeley.TachyonKVExercise$$anonfun$2.apply(TachyonKVExercise.scala:59)
        at scala.collection.TraversableLike$$anonfun$map$1.apply(TraversableLike.scala:244)
        at scala.collection.TraversableLike$$anonfun$map$1.apply(TraversableLike.scala:244)
        at scala.collection.immutable.Range.foreach(Range.scala:141)
        at scala.collection.TraversableLike$class.map(TraversableLike.scala:244)
        at scala.collection.AbstractTraversable.map(Traversable.scala:105)
        at edu.berkeley.TachyonKVExercise$.main(TachyonKVExercise.scala:59)
        at edu.berkeley.TachyonKVExercise.main(TachyonKVExercise.scala)
```
