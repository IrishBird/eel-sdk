package io.eels.component.avro

import org.apache.avro.Schema

/**
  * Avro requires that the types of values passed to its JavaAPI match the types in the schema.
  * So if you have an Avro Boolean field, you can't pass "true" but it must be true. So it's less
  * "forgiving" than JDBC for example.
  *
  * An AvroConverter will convert a JVM type into a suitable Avro type. The avro type being specified by
  * the instances type parameter.
  */
trait AvroConverter[T] {
  def convert(value: Any): T
}

object AvroConverter {
  def apply(`type`: Schema.Type): AvroConverter[_] = `type` match {
    case Schema.Type.LONG => LongConverter
    case Schema.Type.INT => IntConverter
    case Schema.Type.BOOLEAN => BooleanConverter
    case Schema.Type.STRING => StringConverter
    case Schema.Type.DOUBLE => DoubleConverter
    case Schema.Type.FLOAT => FloatConverter
  }
}

class NullableConverter[T](converter: AvroConverter[T]) extends AvroConverter[T] {
  override def convert(value: Any): T = if (value == null) null.asInstanceOf[T] else converter.convert(value)
}

object StringConverter extends AvroConverter[String] {
  override def convert(value: Any): String = value.toString()
}

object BooleanConverter extends AvroConverter[Boolean] {
  override def convert(value: Any): Boolean = value match {
    case b: Boolean => b
    case "true" => true
    case "false" => false
    case _ => sys.error("Could not convert $value to Boolean")
  }
}

object IntConverter extends AvroConverter[Int] {
  override def convert(value: Any): Int = value match {
    case i: Int => i
    case l: Long => l.toInt
    case _ => value.toString().toInt
  }
}

object ShortConverter extends AvroConverter[Short] {
  override def convert(value: Any): Short = value match {
    case s: Short => s
    case i: Int => i.toShort
    case l: Long => l.toShort
    case _ => value.toString().toShort
  }
}

object LongConverter extends AvroConverter[Long] {
  override def convert(value: Any): Long = value match {
    case l: Long => l
    case s: Short => s.toLong
    case i: Int => i.toLong
    case _ => value.toString().toLong
  }
}

object DoubleConverter extends AvroConverter[Double] {
  override def convert(value: Any): Double = value match {
    case d: Double => d
    case f: Float => f.toDouble
    case l: Long => l.toDouble
    case i: Int => i.toDouble
    case _ => value.toString().toDouble
  }
}

object FloatConverter extends AvroConverter[Float] {
  override def convert(value: Any): Float = value match {
    case f: Float => f
    case d: Double => d.toFloat
    case l: Long => l.toFloat
    case i: Int => i.toFloat
    case _ => value.toString().toFloat
  }
}