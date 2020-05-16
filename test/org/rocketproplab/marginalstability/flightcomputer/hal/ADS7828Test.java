package org.rocketproplab.marginalstability.flightcomputer.hal;

import org.junit.Assert;

import java.io.IOException;
import java.util.ArrayList;

import org.junit.Test;
import org.rocketproplab.marginalstability.flightcomputer.Time;
import org.rocketproplab.marginalstability.flightcomputer.comm.PacketRouter;
import org.rocketproplab.marginalstability.flightcomputer.comm.TestPacketListener;

import com.pi4j.io.i2c.I2CDevice;
import java.nio.IntBuffer;
import java.nio.ByteBuffer;

public class ADS7828Test {

  private class MockI2CDevice implements I2CDevice {
    private short sensorReadings[];

    private int address;
    private int chSel;
    private int vrefSel;

    public MockI2CDevice(int addr) {
      address = addr;

      chSel = 0;
      vrefSel = 0;

      sensorReadings = new short[8];
      for (int i = 0; i < 8; i++) {
        sensorReadings[i] = 1234;
      }
    }

    @Override
    public int getAddress() {
      return address;
    }

    /*
     * Extract the bits from the command byte and change
     * the ADC state appropriately
     */
    @Override
    public void write(byte b) throws IOException {
      // Select the ADC channel
      chSel = (b >> 4) & 0x7; 

      // PD1 Flag: VRef selection (0 = extern VRef, 1 = internal 2.5V VRef)
      vrefSel = (b >> 3) & 0x1;
    }

    @Override
    public void write(byte[] buffer, int offset, int size) throws IOException {
      Assert.fail("unimplemented method in unit test for ADS7828");
    }

    @Override
    public void write(byte[] buffer) throws IOException {
      Assert.fail("unimplemented method in unit test for ADS7828");
    }

    @Override
    public void write(int address, byte b) throws IOException {
      Assert.fail("unimplemented method in unit test for ADS7828");
    }

    @Override
    public void write(int address, byte[] buffer, int offset, int size) throws IOException {
      Assert.fail("unimplemented method in unit test for ADS7828");
    }

    @Override
    public void write(int address, byte[] buffer) throws IOException {
      Assert.fail("unimplemented method in unit test for ADS7828");
    }

    @Override
    public int read() throws IOException {
      Assert.fail("unimplemented method in unit test for ADS7828");
      return -1;
    }

    /*
     * TODO Implement
     */
    @Override
    public int read(byte[] buffer, int offset, int size) throws IOException {
      Assert.fail("unimplemented method in unit test for ADS7828");
      return -1;
    }

    @Override
    public int read(int address) throws IOException {
      Assert.fail("unimplemented method in unit test for ADS7828");
      return -1;
    }

    @Override
    public int read(int address, byte[] buffer, int offset, int size)
    throws IOException {
    // TODO IMPLEMENT ME
      Assert.fail("unimplemented method in unit test for ADS7828");
      return -1;
    }

    @Override
    public void ioctl(long command, int value) throws IOException {
      Assert.fail("unimplemented method in unit test for ADS7828");
    }

    @Override
    public void ioctl(long command, ByteBuffer data, IntBuffer offsets)
    throws IOException {
    Assert.fail("unimplemented method in unit test for ADS7828");
    }

    @Override
    public int read(byte[] writeBuffer, int writeOffset, int writeSize,
        byte[] readBuffer, int readOffset, int readSize) throws IOException {
      return -1;
    }
  }

  /**
   * Brute force every I2C address for this sensor, and ensure that
   * invalid addresses throw an exception.
   *
   * TODO: Still need to check if 7-bit address is MSB aligned or
   * LSB aligned, depends on library implementation.
   * Currently assuming MSB aligned
   */
  @Test
  public void testInvalidSensorAddr() {

    // Only 4 addresses are valid, see datasheet
    ArrayList<Integer> validAddresses = new ArrayList<>();
    validAddresses.add(0x90);
    validAddresses.add(0x92);
    validAddresses.add(0x94);
    validAddresses.add(0x96);

    MockI2CDevice i2c;
    ADS7828 adc1;

    // Brute-force 7-bit address space, MSB bits only
    // Ignore LSB because that's the R/nW bit
    for (int addr = 0; addr < (1<<8); addr+=2) {

      i2c = new MockI2CDevice(addr);

      try {
        adc1 = new ADS7828(i2c);

        // If invalid I2C address, should have thrown exception
        if (!validAddresses.contains(addr))
          Assert.fail(String.format("Invalid i2c address (0x%x), did not throw RuntimeException", addr));
      }
      catch (RuntimeException e) {

        // If valid I2C address threw runtime exception, that's bad
        if (validAddresses.contains(addr))
          Assert.fail(String.format("Valid i2c address (0x%x), but threw RuntimeException", addr));
      }
    }
  }

  @Test
  public void testSomething() {
  }
}
