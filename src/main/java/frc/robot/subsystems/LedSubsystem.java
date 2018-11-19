/*----------------------------------------------------------------------------*/
/* Copyright (c) 2017-2018 FIRST. All Rights Reserved.                        */
/* Open Source Software - may be modified and shared by FRC teams. The code   */
/* must be accompanied by the FIRST BSD license file in the root directory of */
/* the project.                                                               */
/*----------------------------------------------------------------------------*/

package frc.robot.subsystems;

import edu.wpi.first.wpilibj.SPI;
import edu.wpi.first.wpilibj.command.Subsystem;

// TODO-DW : create methods to reflect status, joystick, button, team.

/**
 * An example subsystem.  You can replace me with your own Subsystem.
 */
public class LedSubsystem extends Subsystem {
  // Put methods for controlling this subsystem
  // here. Call these from Commands.

  class Pixel {
    byte m_r;
    byte m_g;
    byte m_b;

    public Pixel() {
      m_r = 0;
      m_g = 0;
      m_b  = 0;
    }

    public void copy(Pixel other) {
      m_r = other.m_r;
      m_g = other.m_g;
      m_b = other.m_b;
    }

    public void set(byte r, byte g, byte b) {
      m_r = r;
      m_g = g;
      m_b = b;
    }
  };

  SPI m_spi;
  byte m_power;
  
  static final int NUM_PIXELS = 60;
  static final int MAX_SPI_WRITE = 127;
  Pixel m_pixel[];
  final byte m_frame[] = new byte[NUM_PIXELS*4 + 8];
  byte m_writeBuf[] = new byte[MAX_SPI_WRITE];

  public LedSubsystem() {
    m_power = 1;
    m_pixel = new Pixel[NUM_PIXELS];
    for (int n = 0; n < NUM_PIXELS; n++) {
      m_pixel[n] = new Pixel();
    }

    // Init SPI port
    m_spi = new SPI(SPI.Port.kOnboardCS0);
    m_spi.setClockRate(512000);
    m_spi.setClockActiveLow();
    m_spi.setChipSelectActiveLow();
    m_spi.setSampleDataOnRising();
    m_spi.setMSBFirst();

    blank();
    updateLeds();
  }

  @Override
  public void initDefaultCommand() {
    // Set the default command for a subsystem here.
    // setDefaultCommand(new MySpecialCommand());
  }

  @Override 
  public void periodic() {
    for (int n = NUM_PIXELS-1; n > 0; n--) {
      // shift all pixels by one
      m_pixel[n].copy(m_pixel[n-1]);

      // write one new pixel
      m_pixel[0].set((byte)128, (byte)0, (byte)0);
    }

    updateLeds();
  }

  private void blank() {
    for (int n = 0; n < NUM_PIXELS; n++) {
      m_pixel[n].set((byte)0, (byte)0, (byte)0);
    }
  }

  private void sendFrame() {
    // SPI device can send a max of 128 bytes and that's not enough for a frame
    // of LED data.  So we write it in chunks.
    int cursor = 0;

    for (int n = 0; n < m_frame.length; n++) {
      m_writeBuf[cursor] = m_frame[n];
      cursor += 1;
      if (cursor == MAX_SPI_WRITE) {
        // we have a full write buff, send it now.
        m_spi.write(m_writeBuf, cursor);
        cursor = 0;
      }
    }

    // now write the last part, if any
    if (cursor > 0) {
      m_spi.write(m_writeBuf, cursor);
      cursor = 0;
    }
  }

  private void updateLeds() {
    int wrote = 0;

    // start sequence
    int cursor = 0;
    m_frame[cursor++] = (byte)0;
    m_frame[cursor++] = (byte)0;
    m_frame[cursor++] = (byte)0;
    m_frame[cursor++] = (byte)0;

    // Pixel data
    for (int n = 0; n < NUM_PIXELS; n++) {
      // power multiplier
      m_frame[cursor++] = (byte) (m_power + 0xE0);
      m_frame[cursor++] = m_pixel[n].m_b;
      m_frame[cursor++] = m_pixel[n].m_g;
      m_frame[cursor++] = m_pixel[n].m_r;
    }

    // stop sequence
    m_frame[cursor++] = (byte) 0xFF;
    m_frame[cursor++] = (byte) 0xFF;
    m_frame[cursor++] = (byte) 0xFF;
    m_frame[cursor++] = (byte) 0xFF;

    // send it out the SPI port
    sendFrame();
  }
}
