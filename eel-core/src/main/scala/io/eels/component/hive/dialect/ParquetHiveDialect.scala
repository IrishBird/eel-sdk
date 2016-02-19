package io.eels.component.hive.dialect

import com.typesafe.scalalogging.slf4j.StrictLogging
import io.eels.component.avro.{AvroRecordFn, AvroSchemaGen}
import io.eels.component.hive.{HiveDialect, HiveWriter}
import io.eels.component.parquet.{ParquetIterator, ParquetLogMute}
import io.eels.{FrameSchema, Row}
import org.apache.avro.generic.GenericRecord
import org.apache.hadoop.fs.{FileSystem, Path}
import org.apache.parquet.avro.AvroParquetWriter
import org.apache.parquet.hadoop.ParquetWriter
import org.apache.parquet.hadoop.metadata.CompressionCodecName

object ParquetHiveDialect extends HiveDialect with StrictLogging {

  override def iterator(path: Path, schema: FrameSchema, columns: Seq[String])
                       (implicit fs: FileSystem): Iterator[Row] = new Iterator[Row] {
    ParquetLogMute()

    lazy val iter = ParquetIterator(path, columns)
    override def hasNext: Boolean = iter.hasNext
    override def next(): Row = iter.next
  }

  override def writer(schema: FrameSchema, path: Path)
                     (implicit fs: FileSystem): HiveWriter = {
    ParquetLogMute()
    logger.debug(s"Creating parquet writer for $path")

    val avroSchema = AvroSchemaGen(schema)
    val writer = new AvroParquetWriter[GenericRecord](
      path,
      avroSchema,
      CompressionCodecName.UNCOMPRESSED,
      ParquetWriter.DEFAULT_BLOCK_SIZE,
      ParquetWriter.DEFAULT_PAGE_SIZE
    )
    new HiveWriter {
      override def close(): Unit = writer.close()
      override def write(row: Row): Unit = {
        val record = AvroRecordFn.toRecord(row, avroSchema)
        writer.write(record)
      }
    }
  }
}