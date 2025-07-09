import React from 'react';
import {
  Modal,
  View,
  Text,
  TouchableOpacity,
  StyleSheet,
  SafeAreaView,
} from 'react-native';

interface PermissionModalProps {
  visible: boolean;
  onConfirm: () => void;
}

const PermissionModal: React.FC<PermissionModalProps> = ({ visible, onConfirm }) => {
  return (
    <Modal
      animationType="fade"
      transparent={true}
      visible={visible}
      onRequestClose={() => {}} // No action on back button press
    >
      <SafeAreaView style={styles.centeredView}>
        <View style={styles.modalView}>
          <Text style={styles.modalTitle}>Enable Trip Alerts</Text>
          <Text style={styles.modalText}>
            To see new trip requests instantly, the app needs one permission.
          </Text>
          <Text style={styles.modalText}>
            Tap below, find <Text style={{fontWeight: 'bold'}}>AwesomeProject</Text> in the list, and <Text style={{fontWeight: 'bold'}}>turn the switch on</Text>.
          </Text>
          <TouchableOpacity
            style={styles.button}
            onPress={onConfirm}
          >
            <Text style={styles.textStyle}>Go to Settings</Text>
          </TouchableOpacity>
        </View>
      </SafeAreaView>
    </Modal>
  );
};

const styles = StyleSheet.create({
  centeredView: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    backgroundColor: 'rgba(0, 0, 0, 0.6)', // Dark, semi-transparent background
  },
  modalView: {
    margin: 20,
    backgroundColor: 'white',
    borderRadius: 20,
    padding: 35,
    alignItems: 'center',
    shadowColor: '#000',
    shadowOffset: {
      width: 0,
      height: 2,
    },
    shadowOpacity: 0.25,
    shadowRadius: 4,
    elevation: 5,
    width: '90%',
  },
  modalTitle: {
    fontSize: 22,
    fontWeight: 'bold',
    marginBottom: 15,
    textAlign: 'center',
    color: '#333',
  },
  modalText: {
    marginBottom: 15,
    textAlign: 'center',
    fontSize: 16,
    lineHeight: 24,
    color: '#555',
  },
  button: {
    borderRadius: 10,
    paddingVertical: 12,
    paddingHorizontal: 30,
    backgroundColor: '#007AFF', // A modern blue
    marginTop: 10,
  },
  textStyle: {
    color: 'white',
    fontWeight: 'bold',
    textAlign: 'center',
    fontSize: 16,
  },
});

export default PermissionModal;
