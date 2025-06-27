// React and RN components.
import React, { useState, useEffect } from 'react';
import { View, Button, StyleSheet, Text, ScrollView, SafeAreaView } from 'react-native';

// Our overlay logic.
import OverlayService from '../../modules/OverlayService';
import OverlayPermission from '../../modules/OverlayPermission';

// Demo screen for testing the overlay system.
const OverlayDemoScreen = () => {
  // State for on-screen logs.
  const [logs, setLogs] = useState<string[]>([]);

  // Helper to add a message to the on-screen log.
  const log = (message: string) => {
    console.log(message); // Also log to Metro console.
    setLogs(prev => [`[${new Date().toLocaleTimeString()}] ${message}`, ...prev]);
  };

  // On component mount, set up event listeners.
  useEffect(() => {
    log('Demo screen loaded.');

    // --- Event Handlers ---
    const onTripAccepted = (event: { finalFare?: number }) => {
      log(`Event: Trip Accepted! Final Fare: ₹${event?.finalFare?.toFixed(2) ?? 'N/A'}`);
    };
    const onTripIgnored = () => log('Event: Trip Ignored!');
    const onBubbleClicked = () => log('Event: Bubble Clicked! Service will show card.');

    // Subscribe to service events.
    OverlayService.addEventListener('onTripAccepted', onTripAccepted);
    OverlayService.addEventListener('onTripIgnored', onTripIgnored);
    OverlayService.addEventListener('onBubbleClicked', onBubbleClicked);

    // Cleanup on component unmount.
    return () => {
      log('Cleaning up listeners.');
      OverlayService.removeEventListener('onTripAccepted', onTripAccepted);
      OverlayService.removeEventListener('onTripIgnored', onTripIgnored);
      OverlayService.removeEventListener('onBubbleClicked', onBubbleClicked);
    };
  }, []);

  // --- Button Actions ---

  // Check overlay permission status.
  const checkPermission = async () => {
    log('Checking permission...');
    try {
      const hasPermission = await OverlayPermission.checkOverlayPermission();
      log(`Permission status: ${hasPermission ? 'Granted' : 'Denied'}`);
    } catch (e: any) {
      log(`Error: ${e.message}`);
    }
  };

  // Request overlay permission from user.
  const requestPermission = async () => {
    log('Requesting permission...');
    try {
      await OverlayPermission.requestOverlayPermission();
      log('Permission dialog shown.');
    } catch (e: any) {
      log(`Error: ${e.message}`);
    }
  };

  // Show the floating bubble.
  const showBubble = () => {
    log('Showing bubble...');
    // The service caches this data to show the card later.
    const tripData = {
      destination: '123 Market Street, Downtown',
      totalFare: '₹50',
      distance: '5.2 miles',
      eta: '15 mins',
    };
    OverlayService.showBubble(1, tripData).catch(e => log(`Error: ${e.message}`))
  };

  // Show the full trip card directly.
  const showTripCard = () => {
    log('Showing trip card...');
    const tripData = {
      destination: '123 Market Street, Downtown',
      totalFare: '₹50',
      distance: '5.2 miles',
      eta: '15 mins',
    };
    OverlayService.showTripCard(tripData).catch(e => log(`Error: ${e.message}`));
  };

  // Hide any visible overlay.
  const hideOverlay = () => {
    log('Hiding overlay...');
    OverlayService.hideOverlay().catch(e => log(`Error: ${e.message}`))
  };

  // JSX for the screen UI.
  return (
    <SafeAreaView style={styles.safeArea}>
      <View style={styles.container}>
        <Text style={styles.title}>Overlay Demo</Text>
        {/* Control buttons */}
        <View style={styles.controls}>
          <Button title="Check Permission" onPress={checkPermission} />
          <Button title="Request Permission" onPress={requestPermission} />
          <Button title="Show Bubble" onPress={showBubble} />
          <Button title="Show Trip Card" onPress={showTripCard} />
          <Button title="Hide Overlay" onPress={hideOverlay} color="#FF6347" />
        </View>
        {/* Log display area */}
        <View style={styles.logContainer}>
          <Text style={styles.logTitle}>Logs</Text>
          <ScrollView style={styles.logScrollView} contentContainerStyle={{ padding: 5 }}>
            {/* Map logs array to Text components */}
            {logs.map((msg, index) => (
              <Text key={index} style={styles.logText}>{msg}</Text>
            ))}
          </ScrollView>
        </View>
      </View>
    </SafeAreaView>
  );
};

// Component styles.
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
    fontFamily: 'monospace', // Monospaced for logs.
  },
});

// Export component for use in App.tsx.
export default OverlayDemoScreen;
