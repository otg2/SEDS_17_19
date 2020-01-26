import tensorflow as tf
import numpy as np
import matplotlib.pyplot as plt
import time
import os
from PIL import Image

from flask import Flask
app = Flask(__name__)

import tensorflow as tf



IMAGE_HEIGHT = 32
IMAGE_WIDTH = 32
IMAGE_CHANNELS = 3

data_path = 'data/'


# create some wrappers for simplicity
def conv2d(x,W,b,strides=1):
    # conv2d wrapper, with bias and relu activation
    x = tf.nn.conv2d(x,W,strides=[1,strides,strides,1],padding='SAME')
    # x = tf.nn.conv3d(x,W,strides=[1,strides,strides,strides,1],padding='SAME')
    x = tf.nn.bias_add(x,b)
    return tf.nn.relu(x)

def maxpool2d(x,k=2):
    # max2d wrapper
    return tf.nn.max_pool(x,ksize=[1,k,k,1],strides=[1,k,k,1],padding='SAME')

def conv_net(X,weights,biases,dropout):
    X = tf.reshape(X, shape=[-1,IMAGE_HEIGHT,IMAGE_WIDTH,IMAGE_CHANNELS])

    # convolustion layer
    conv1 = conv2d(X,weights['wc1'],biases['bc1'])
    # max pooling (down-sampling)
    conv1 = maxpool2d(conv1, k=2)

    # convolustion layer
    conv2 = conv2d(conv1, weights['wc2'], biases['bc2'])
    # max pooling (down-sampling)
    conv2 = maxpool2d(conv2, k=2)
    
    # apply dropout
    # conv2 = tf.nn.dropout(conv2, 0.98)

    # convolustion layer
    conv3 = conv2d(conv2, weights['wc3'], biases['bc3'])
    # max pooling (down-sampling)
    conv3 = maxpool2d(conv3, k=2)
    
    # apply dropout
    # conv3 = tf.nn.dropout(conv3, 0.95)

    # convolustion layer
    conv4 = conv2d(conv3, weights['wc4'], biases['bc4'])
    # max pooling (down-sampling)
    conv4 = maxpool2d(conv4, k=2)
    
    # apply dropout
    # conv4 = tf.nn.dropout(conv4, 0.9)

    # convolustion layer
    conv5 = conv2d(conv4, weights['wc5'], biases['bc5'])
    # max pooling (down-sampling)
    conv5 = maxpool2d(conv5, k=2)
    
    # apply dropout
    conv5 = tf.nn.dropout(conv5, 0.9)

    # print(conv4.shape)
    # fully connected layer
    fc1 = tf.reshape(conv5, shape=[-1,weights['wd1'].get_shape().as_list()[0]])
    # print('conv4 shape:', conv4.shape, ', fc1 shape:', fc1.shape)
    fc1 = tf.add(tf.matmul(fc1,weights['wd1']), biases['bd1'])
    fc1 = tf.nn.relu(fc1)

    # apply dropout
    fc1 = tf.nn.dropout(fc1, dropout)

    # output, class prediction
    out = tf.add(tf.matmul(fc1,weights['out']), biases['out'])
    return  out
	
# parameters
lr_start = 0.001
lr_end = 0.0001
learning_rate = lr_start

num_steps = 5000
batch_size = 64
update_step = 5
display_step = 100
train_acc_target = 1

# network parameters
num_input = IMAGE_HEIGHT*IMAGE_WIDTH*IMAGE_CHANNELS
num_classes = 10 # len(fruits_dict)
dropout = 0.5

# saver train parameters
useCkpt = True
checkpoint_step = 5
checkpoint_dir = os.getcwd()+'\\checkpoint\\'

# store layers weighta and bias
weights = {
    # 5x5 conv, 3 inputs, 16 outpus
    'wc1': tf.get_variable('wc1',[3,3,3,32],initializer=tf.contrib.layers.xavier_initializer_conv2d()),
    # 5x5 conv, 16 input, 32 outpus
    'wc2': tf.get_variable('wc2',[3,3,32,64],initializer=tf.contrib.layers.xavier_initializer_conv2d()),
    # 5x5 conv, 32 inputs, 64 outputs
    'wc3': tf.get_variable('wc3',[3,3,64,128],initializer=tf.contrib.layers.xavier_initializer_conv2d()),
    # 5x5 conv, 64 inputs, 128 outputs
    'wc4': tf.get_variable('wc4',[3,3,128,256],initializer=tf.contrib.layers.xavier_initializer_conv2d()),
    # 5x5 conv, 128 inputs, 256 outputs
    'wc5': tf.get_variable('wc5', [3, 3, 256, 512], initializer=tf.contrib.layers.xavier_initializer_conv2d()),

    # fully connected, 7*7*128 inputs, 2048 outputs
    'wd1': tf.get_variable('wd1',[1*1*512,2048],initializer=tf.contrib.layers.xavier_initializer()),
    # 32 inputs, 26 outputs (class prediction)
    'out': tf.get_variable('fc1',[2048,num_classes],initializer=tf.contrib.layers.xavier_initializer()),
}
biases = {
    'bc1': tf.Variable(tf.zeros([32])),
    'bc2': tf.Variable(tf.zeros([64])),
    'bc3': tf.Variable(tf.zeros([128])),
    'bc4': tf.Variable(tf.zeros([256])),
    'bc5': tf.Variable(tf.zeros([512])),
    'bd1': tf.Variable(tf.zeros([2048])),
    'out': tf.Variable(tf.zeros([num_classes]))
}

# tf graph input
X = tf.placeholder(tf.float32,[None,num_input])
Y = tf.placeholder(tf.float32,[None,num_classes])
keep_prob = tf.placeholder(tf.float32)

# cconstruct model
logits = conv_net(X,weights,biases,keep_prob)
prediction = tf.nn.softmax(logits)


correct_pred_index = tf.argmax(prediction,1)

# initialization
init = tf.global_variables_initializer()
saver = tf.train.Saver()

from scipy import misc



def runNet():

	with tf.Session() as sess:
		sess.run(init)
		# create coord
		coord2 = tf.train.Coordinator()
		threads2 = tf.train.start_queue_runners(sess=sess, coord=coord2)

		if useCkpt:
			ckpt = tf.train.get_checkpoint_state(checkpoint_dir)
			if ckpt and ckpt.model_checkpoint_path:
				saver.restore(sess, ckpt.model_checkpoint_path)
				print("Checkpoint found ",str(ckpt.model_checkpoint_path))
			else:
				pass
			
		# Run 1 single image
		arr = misc.imread('IMG_TO_CLASSIFY.jpg') 
		imgShape = np.shape(arr)
		if(imgShape[2] == 4):
			arr = arr[:,:,:3]
		arr = np.divide(arr,255) - 0.5
		imgToCheck = arr
		labelArr = np.zeros(shape=[1, num_classes])
		imgToCheck = np.reshape(imgToCheck, [1, num_input])
		testPrediction = sess.run(correct_pred_index, feed_dict={X: imgToCheck, Y: labelArr, keep_prob: 1})
		
		return str(testPrediction)
		# close coord
		coord2.request_stop()
		coord2.join(threads2)
		sess.close()
		return "gurk1"


@app.route("/")
def hello():
    return runNet()
	
@app.route('/api', methods=['POST'])
def api():
    return runNet()

	
if __name__ == "__main__":
    app.run()	