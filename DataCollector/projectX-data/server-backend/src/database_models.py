
from sqlalchemy import Column, String, ForeignKey, Float, DateTime, BigInteger, Integer
from sqlalchemy.orm import relationship
from sqlalchemy.ext.declarative import declarative_base
from sqlalchemy.dialects.postgresql import UUID

Base = declarative_base()


class TestDescription(Base):
    __tablename__ = 'test_descriptions'

    description_id = Column(UUID(as_uuid=True), primary_key=True)
    description = Column(String)
    model = Column(String)
    timestamp = Column(DateTime)
    username = Column(String)

    tests = relationship("Test", back_populates="description", cascade="all, delete-orphan")


class Activity(Base):
    __tablename__ = 'activities'

    activity_id = Column(BigInteger, primary_key=True, autoincrement=True)
    activity = Column(String)

    tests = relationship("Test", back_populates="activity", cascade="all, delete-orphan")


class Test(Base):
    __tablename__ = 'tests'

    test_id = Column(UUID(as_uuid=True), primary_key=True)
    activity_id = Column(BigInteger, ForeignKey('activities.activity_id', ondelete="CASCADE"), nullable=False)
    description_id = Column(UUID(as_uuid=True), ForeignKey('test_descriptions.description_id', ondelete="CASCADE"), nullable=False)

    activity = relationship("Activity", back_populates="tests")
    description = relationship("TestDescription", back_populates="tests")

    accelerometer_readings = relationship("Accelerometer", back_populates="test", cascade="all, delete-orphan")
    gyroscope_readings = relationship("Gyroscope", back_populates="test", cascade="all, delete-orphan")
    magnetic_field_readings = relationship("MagneticField", back_populates="test", cascade="all, delete-orphan")
    gravity_readings = relationship("Gravity", back_populates="test", cascade="all, delete-orphan")
    proximity_readings = relationship("Proximity", back_populates="test", cascade="all, delete-orphan")
    pressure_readings = relationship("Pressure", back_populates="test", cascade="all, delete-orphan")
    linear_acceleration_readings = relationship("LinearAcceleration", back_populates="test", cascade="all, delete-orphan")
    rotation_vector_readings = relationship("RotationVector", back_populates="test", cascade="all, delete-orphan")
    wifi_readings = relationship("Wifi", back_populates="test", cascade="all, delete")
    location_readings = relationship("Location", back_populates="test", cascade="all, delete")


class Accelerometer(Base):
    __tablename__ = "accelerometer"
    id = Column(BigInteger, primary_key=True, autoincrement=True)
    timestamp = Column(DateTime)
    x = Column(Float)
    y = Column(Float)
    z = Column(Float)
    test_id = Column(UUID(as_uuid=True), ForeignKey('tests.test_id', ondelete="CASCADE"), nullable=False)

    test = relationship("Test", back_populates="accelerometer_readings")


class Gyroscope(Base):
    __tablename__ = "gyroscope"
    id = Column(BigInteger, primary_key=True, autoincrement=True)
    timestamp = Column(DateTime)
    x = Column(Float)
    y = Column(Float)
    z = Column(Float)
    test_id = Column(UUID(as_uuid=True), ForeignKey('tests.test_id', ondelete="CASCADE"), nullable=False)

    test = relationship("Test", back_populates="gyroscope_readings")


class MagneticField(Base):
    __tablename__ = "magnetic_field"
    id = Column(BigInteger, primary_key=True, autoincrement=True)
    timestamp = Column(DateTime)
    x = Column(Float)
    y = Column(Float)
    z = Column(Float)
    test_id = Column(UUID(as_uuid=True), ForeignKey('tests.test_id', ondelete="CASCADE"), nullable=False)

    test = relationship("Test", back_populates="magnetic_field_readings")


class Gravity(Base):
    __tablename__ = "gravity"
    id = Column(BigInteger, primary_key=True, autoincrement=True)
    timestamp = Column(DateTime)
    x = Column(Float)
    y = Column(Float)
    z = Column(Float)
    test_id = Column(UUID(as_uuid=True), ForeignKey('tests.test_id', ondelete="CASCADE"), nullable=False)

    test = relationship("Test", back_populates="gravity_readings")


class Proximity(Base):
    __tablename__ = "proximity"
    id = Column(BigInteger, primary_key=True, autoincrement=True)
    timestamp = Column(DateTime)
    distance_cm = Column(Float)
    test_id = Column(UUID(as_uuid=True), ForeignKey('tests.test_id', ondelete="CASCADE"), nullable=False)

    test = relationship("Test", back_populates="proximity_readings")


class Pressure(Base):
    __tablename__ = "pressure"
    id = Column(BigInteger, primary_key=True, autoincrement=True)
    timestamp = Column(DateTime)
    pressure_hpa = Column(Float)
    test_id = Column(UUID(as_uuid=True), ForeignKey('tests.test_id', ondelete="CASCADE"), nullable=False)

    test = relationship("Test", back_populates="pressure_readings")


class LinearAcceleration(Base):
    __tablename__ = "linear_acceleration"
    id = Column(BigInteger, primary_key=True, autoincrement=True)
    timestamp = Column(DateTime)
    x = Column(Float)
    y = Column(Float)
    z = Column(Float)
    test_id = Column(UUID(as_uuid=True), ForeignKey('tests.test_id', ondelete="CASCADE"), nullable=False)

    test = relationship("Test", back_populates="linear_acceleration_readings")


class RotationVector(Base):
    __tablename__ = "rotation_vector"
    id = Column(BigInteger, primary_key=True, autoincrement=True)
    timestamp = Column(DateTime)
    x = Column(Float)
    y = Column(Float)
    z = Column(Float)
    cos = Column(Float)
    test_id = Column(UUID(as_uuid=True), ForeignKey('tests.test_id', ondelete="CASCADE"), nullable=False)

    test = relationship("Test", back_populates="rotation_vector_readings")


class Wifi(Base):
    __tablename__ = "wifi"

    id = Column(BigInteger, primary_key=True, autoincrement=True)
    timestamp = Column(DateTime)
    ssid = Column(String)
    bssid = Column(String)
    level = Column(Integer)
    frequency = Column(Integer)
    test_id = Column(UUID(as_uuid=True), ForeignKey('tests.test_id', ondelete="CASCADE"), nullable=False)

    test = relationship("Test", back_populates="wifi_readings")


class Location(Base):
    __tablename__ = "location"

    id = Column(BigInteger, primary_key=True, autoincrement=True)
    timestamp = Column(DateTime)
    latitude = Column(Float)
    longitude = Column(Float)
    altitude = Column(Float)
    vertical_accuracy = Column(Float, nullable=True)
    horizontal_accuracy = Column(Float, nullable=True)
    msl_altitude = Column(Float, nullable=True)
    msl_altitude_accuracy = Column(Float, nullable=True)
    speed = Column(Float, nullable=True)
    speed_accuracy = Column(Float, nullable=True)
    bearing = Column(Float, nullable=True)
    bearing_accuracy = Column(Float, nullable=True)
    provider = Column(String)
    test_id = Column(UUID(as_uuid=True), ForeignKey('tests.test_id', ondelete="CASCADE"))

    test = relationship("Test", back_populates="location_readings")
