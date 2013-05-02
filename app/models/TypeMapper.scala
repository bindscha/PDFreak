package models

import scala.util.matching.Regex

import scala.slick._
import scala.slick.driver._
import scala.slick.lifted.{ MappedTypeMapper => Mapper, _ }
import scala.slick.session._

object TypeMapper {

  implicit val datetime2timestamp =
    Mapper.base[org.joda.time.DateTime, java.sql.Timestamp](
      dt => new java.sql.Timestamp(dt.getMillis),
      ts => new org.joda.time.DateTime(ts.getTime))

  implicit object PostgresByteArrayTypeMapper extends BaseTypeMapper[Array[Byte]] with TypeMapperDelegate[Array[Byte]] {
    def apply(p: BasicProfile) = this
    val zero = new Array[Byte](0)
    val sqlType = java.sql.Types.BLOB
    override val sqlTypeName = "BYTEA"
    def setValue(v: Array[Byte], p: PositionedParameters) {
      p.pos += 1
      p.ps.setBytes(p.pos, v)
    }
    def setOption(v: Option[Array[Byte]], p: PositionedParameters) {
      p.pos += 1
      if (v eq None) p.ps.setBytes(p.pos, null) else p.ps.setBytes(p.pos, v.get)
    }
    def nextValue(r: PositionedResult) = {
      r.nextBytes()
    }
    def updateValue(v: Array[Byte], r: PositionedResult) {
      r.updateBytes(v)
    }
    override def valueToSQLLiteral(value: Array[Byte]) =
      throw new SlickException("Cannot convert BYTEA to literal")

  }

}