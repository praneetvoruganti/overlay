import React, { useState, useEffect } from 'react';
import { View, Button, StyleSheet, Text, ScrollView, SafeAreaView } from 'react-native';
import OverlayService from '../../modules/OverlayService';
import OverlayPermission from '../../modules/OverlayPermission';

const OverlayDemoScreen = () => {
  const [logs, setLogs] = useState<string[]>([]);

  const log = (message: string) => {
    console.log(message);
    setLogs(prev => [`[${new Date().toLocaleTimeString()}] ${message}`, ...prev]);
  };

  useEffect(() => {
    log('Demo screen initialized.');
  }, []);

  const checkPermission = async () => {
    log('Checking overlay permission...');
    try {
      const hasPermission = await OverlayPermission.checkOverlayPermission();
      log(`Permission status: ${hasPermission ? 'Granted' : 'Denied'}`);
    } catch (e: any) {
      log(`Error checking permission: ${e.message}`);
    }
  };

  const requestPermission = async () => {
    log('Requesting overlay permission...');
    try {
      await OverlayPermission.requestOverlayPermission();
      log('Permission request sent. Check status again.');
    } catch (e: any) {
      log(`Error requesting permission: ${e.message}`);
    }
  };

  const showBubble = () => {
    log('Show Bubble pressed');
    const tripData = {
      fare: '$25.50',
      pickup: '123 Main St',
      dropoff: '456 Oak Ave',
    };
    OverlayService.showBubble(1, tripData).catch(e => log(`Error: ${e.message}`))
  };

  const showTripCard = () => {
    log('Show Trip Card pressed');
    const tripData = {
      fare: '$30.00',
      pickup: '789 Pine Ln',
      dropoff: '101 Maple Dr',
    };
    OverlayService.showTripCard(tripData).catch(e => log(`Error: ${e.message}`))
  };

  const hideOverlay = () => {
    log('Hide Overlay pressed');
    OverlayService.hideOverlay().catch(e => log(`Error: ${e.message}`))
  };

  return (
    <SafeAreaView style={styles.safeArea}>
      <View style={styles.container}>
        <Text style={styles.title}>Overlay Demo</Text>
        <View style={styles.controls}>
          <Button title="Check Permission" onPress={checkPermission} />
          <Button title="Request Permission" onPress={requestPermission} />
          <Button title="Show Bubble" onPress={showBubble} />
          <Button title="Show Trip Card" onPress={showTripCard} />
          <Button title="Hide Overlay" onPress={hideOverlay} color="#FF6347" />
        </View>
        <View style={styles.logContainer}>
          <Text style={styles.logTitle}>Logs</Text>
          <ScrollView style={styles.logScrollView} contentContainerStyle={{ padding: 5 }}>
            {logs.map((msg, index) => (
              <Text key={index} style={styles.logText}>{msg}</Text>
            ))}
          </ScrollView>
        </View>
      </View>
    </SafeAreaView>
  );
};

const styles = StyleSheet.create({
  safeArea: { flex: 1, backgroundColor: '#F5FCFF' },
  container: {
    flex: 1,
    padding: 10,
  },
  title: {
    fontSize: 24,
    fontWeight: 'bold',
    textAlign: 'center',
    marginVertical: 10,
  },
  controls: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    justifyContent: 'center',
    gap: 10,
    marginBottom: 10,
  },
  logContainer: {
    flex: 1,
    borderWidth: 1,
    borderColor: '#CCC',
    borderRadius: 5,
    marginTop: 10,
  },
  logTitle: {
    fontSize: 16,
    fontWeight: 'bold',
    padding: 5,
    borderBottomWidth: 1,
    borderColor: '#CCC',
    backgroundColor: '#F0F0F0',
  },
  logScrollView: {
    flex: 1,
  },
  logText: {
    fontSize: 12,
    fontFamily: 'monospace',
  },
});

export default OverlayDemoScreen;
