package org.intermine.model;

import org.intermine.model.InterMineId;
import java.lang.Number;
import java.lang.Integer;
import java.sql.Types;

public class InterMineId extends Number implements Comparable <InterMineId>{
  
  Integer value;
  public static int MAX_VALUE = Integer.MAX_VALUE;
  public static int MIN_VALUE = Integer.MIN_VALUE;
  public static int SIZE = Integer.SIZE;
  public static Class<Integer> TYPE = Integer.TYPE;
  public static String JDBC_TYPE = "INTEGER";
  public static int SQL_TYPE = java.sql.Types.INTEGER;
  
  public InterMineId(Number i) {
    value = new Integer(i.intValue());
  }
  public InterMineId(String s) {
    value = new Integer(Integer.parseInt(s));
  }
  public int nativeValue() {
    return value.intValue();
  }
  @Override
  public double doubleValue() {
    return value.doubleValue();
  }
  @Override
  public float floatValue() {
    return value.floatValue();
  }
  @Override
  public int intValue() {
    return value.intValue();
  }
  @Override
  public byte byteValue() {
    return value.byteValue();
  }
  @Override
  public short shortValue() {
    return value.shortValue();
  }
  @Override
  public long longValue() {
    return value.longValue();
  }
  @Override
  public String toString() {
    return value.toString();
  }
  @Override
  public boolean equals(Object obj) {
    if (obj instanceof Number ) {
      return longValue() == ((Number)obj).longValue();
    } else {
      return false;
    }

  }
  public void add(int i) {
    value = value + i;
  }
  public int compareTo(InterMineId anotherInterMineId) {
    return value.compareTo(new Integer(anotherInterMineId.intValue()));
  }
  public int compareTo(int x) {
    return compare(value.intValue(),x);
  }
  public int compareTo(long x) {
    return compare(value.longValue(),x);
  }
  @Override
  public int hashCode() {
    return value.hashCode();
  }
  public static Integer decode(String nm) {
    return Integer.decode(nm);
  }
  public static Integer getInteger(String nm) {
    return Integer.getInteger(nm);
  }
  public static Integer getInteger(String nm, Integer val) {
    return Integer.getInteger(nm,val);
  }
  public static Integer getInteger(String nm, int val) {
    return Integer.getInteger(nm,val);
  }
  public static InterMineId getInterMineId(String nm, InterMineId val) {
    return InterMineId.getInterMineId(nm,val);
  }
  public static InterMineId getInterMineId(String nm, int val) {
    return InterMineId.getInterMineId(nm,val);
  }
  public static InterMineId valueOf(String s) {
    return InterMineId.valueOf(Integer.parseInt(s));
  }
  public static InterMineId valueOf(String s, int radix) {
    return InterMineId.valueOf(Integer.valueOf(s,radix));
  }
  public static InterMineId valueOf(int i) {
    return new InterMineId(i);
  }  
  public static InterMineId valueOf(Integer i) {
    return new InterMineId(i.intValue());
  }
  public static InterMineId valueOf(InterMineId i) {
    return new InterMineId(i);
  }
  public static String toBinaryString(int i) {
    return Integer.toBinaryString(i);
  }
  public static String toHexString(int i) {
    return Integer.toHexString(i);
  }
  public static String toOctalString(int i) {
    return Integer.toOctalString(i);
  }
  public static String toString(int i) {
    return Integer.toString(i);
  }
  public static String toString(int i, int radix) {
    return Integer.toString(i,radix);
  }
  public static int bitCount(int i) {
    return Integer.bitCount(i);
  }
  public static int compare(int x, int y) {
    return Integer.compare(x,y);
  }
  public static int compare(long x, long y) {
    return Long.compare(x,y);
  }
  public static int highestOneBit(int i) {
    return Integer.highestOneBit(i);
  }
  public static int lowestOneBit(int i) {
    return Integer.lowestOneBit(i);
  }
  public static int numberOfLeadingZeros(int i) {
    return Integer.numberOfLeadingZeros(i);
  }
  public static int numberOfTrailingZeros(int i) {
    return Integer.numberOfTrailingZeros(i);
  }
  public static int parseInt(String s) {
    return Integer.parseInt(s);
  }
  public static int parseInt(String s, int radix) {
    return Integer.parseInt(s,radix);
  }
  public static int reverse(int i) {
    return Integer.reverse(i);
  }
  public static int reverseBytes(int i) {
    return Integer.reverseBytes(i);
  }
  public static int rotateLeft(int i, int distance) {
    return Integer.rotateLeft(i,distance);
  }
  public static int rotateRight(int i, int distance) {
    return Integer.rotateRight(i,distance);
  }
  public static int signum(int i) {
    return Integer.signum(i);
  }
}
