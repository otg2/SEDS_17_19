

import os
# remove version error notification
os.environ['TF_CPP_MIN_LOG_LEVEL']='2'
import tensorflow as tf
import numpy as np
import random
import scipy.misc
from utils import *
import matplotlib.pyplot as plt

height, width, depth = 128, 128, 3
# Change to 25 (5x5)
batchSize = 25
epoch = 4500

generatedPath = '/output/our' 

def imagesToTensor():   
    # Get the current working directory
    cwd = os.getcwd()
    dataDir = os.path.join(cwd, 'data')
    imageArray = []
    # Find all images
    for each in os.listdir(dataDir):
        imageArray.append(os.path.join(dataDir,each))
    
    foundImages = len(imageArray)
    
    # images to tensor 
    # http://ischlag.github.io/2016/06/19/tensorflow-input-pipeline-example/
    # convert string files into tensors
    arrayToTensor = tf.convert_to_tensor(imageArray, dtype = tf.string)
    
    # slice our tensors into single instances and queue them up using threads
    tensorArray = tf.train.slice_input_producer([arrayToTensor])
    
    # Decode content to jpeg file
    content = tf.read_file(tensorArray[0])
    print("Original tensor form: ")
    print(content)
    imageTensor = tf.image.decode_jpeg(content, channels = depth)
   
    # Add random seed to get different results
    tf.set_random_seed(108)
    
    # Add noise to images to make sure we never train on the same data
    imageTensor = tf.image.random_brightness(imageTensor, max_delta = 0.1)
    imageTensor = tf.image.random_flip_left_right(imageTensor)
    imageTensor = tf.image.random_contrast(imageTensor, lower = 0.95, upper = 1.05)
    
    size = [height, width]
    
    # Make sure all images are in correct size
    imageTensor = tf.image.resize_images(imageTensor, size)
    # Set the dimensions of NxMxO (width, height and depth)
    imageTensor.set_shape([height,width,depth])
    
    # Set the data type
    imageTensor = tf.cast(imageTensor, tf.float32)
    # Normalize RGB
    imageTensor = imageTensor / 255.0
    
    print("Final tensor form: ")
    print(imageTensor)
    
    # Creates batches by randomly shuffling tensors.
    shuffledImageBatch = tf.train.shuffle_batch(
                                    [imageTensor],                    # The list or dictionary of tensors to enqueue.
                                    batch_size = batchSize,          # The new batch size pulled from the queue
                                    num_threads = 5,                  # The number of threads enqueuing tensor_list
                                    capacity = 10000,                 # An integer. The maximum number of elements in the queue.
                                    min_after_dequeue = 100)          # Minimum number elements in the queue. Ensure a level of mixing of elements.
    
    return shuffledImageBatch, foundImages
     
# https://cdn-images-1.medium.com/max/2000/1*39Nnni_nhPDaLu9AnTLoWw.png
# The ReLU activation (Nair & Hinton, 2010) is used in the generator with the exception of the output layer which uses the Tanh function. 
#https://stackoverflow.com/questions/41489907/generative-adversarial-networks-tanh
def generator(input, aRandomDimension, is_train, reuse=False):
    
    output_dim = depth  # RGB image
    with tf.variable_scope('gen') as scope:
        if reuse:
            scope.reuse_variables()
        w1 = tf.get_variable('w1', shape=[aRandomDimension, 4 * 4 * 512], dtype=tf.float32, initializer=tf.truncated_normal_initializer(stddev=0.02))
        b1 = tf.get_variable('b1', shape=[4 * 4 * 512], dtype=tf.float32,initializer=tf.constant_initializer(0.0))
        flat_conv1 = tf.add(tf.matmul(input, w1), b1, name='flat_conv1')
        
        conv1 = tf.reshape(flat_conv1, shape=[-1, 4, 4, 512], name='conv1')
        bn1 = tf.contrib.layers.batch_norm(conv1, is_training=is_train, epsilon=1e-5, decay = 0.9,  updates_collections=None, scope='bn1')
        act1 = tf.nn.relu(bn1, name='act1')
		
        # Create a transpose between layers
		# https://www.tensorflow.org/api_docs/python/tf/layers/conv2d_transpose
		
		# Create convolutional layer
		# https://www.tensorflow.org/api_docs/python/tf/contrib/layers/batch_norm
        # 8*8*256
        conv2 = tf.layers.conv2d_transpose(act1, 256, kernel_size=[5, 5], strides=[2, 2], padding="SAME",kernel_initializer=tf.truncated_normal_initializer(stddev=0.02),name='conv2')
        bn2 = tf.contrib.layers.batch_norm(conv2, is_training=is_train, epsilon=1e-5, decay = 0.9,  updates_collections=None, scope='bn2')
        act2 = tf.nn.relu(bn2, name='act2')
        
        # 16*16*128
        conv3 = tf.layers.conv2d_transpose(act2, 128, kernel_size=[5, 5], strides=[2, 2], padding="SAME",kernel_initializer=tf.truncated_normal_initializer(stddev=0.02),name='conv3')
        bn3 = tf.contrib.layers.batch_norm(conv3, is_training=is_train, epsilon=1e-5, decay = 0.9,  updates_collections=None, scope='bn3')
        act3 = tf.nn.relu(bn3, name='act3')
        
        # 32*32*64
        conv4 = tf.layers.conv2d_transpose(act3, 64, kernel_size=[5, 5], strides=[2, 2], padding="SAME",kernel_initializer=tf.truncated_normal_initializer(stddev=0.02),name='conv4')
        bn4 = tf.contrib.layers.batch_norm(conv4, is_training=is_train, epsilon=1e-5, decay = 0.9,  updates_collections=None, scope='bn4')
        act4 = tf.nn.relu(bn4, name='act4')
        
        # 64*64*32
        conv5 = tf.layers.conv2d_transpose(act4, 32, kernel_size=[5, 5], strides=[2, 2], padding="SAME",kernel_initializer=tf.truncated_normal_initializer(stddev=0.02),name='conv5')
        bn5 = tf.contrib.layers.batch_norm(conv5, is_training=is_train, epsilon=1e-5, decay = 0.9,  updates_collections=None, scope='bn5')
        act5 = tf.nn.relu(bn5, name='act5')
        
        #128*128*3
        conv6 = tf.layers.conv2d_transpose(act5, output_dim, kernel_size=[5, 5], strides=[2, 2], padding="SAME",kernel_initializer=tf.truncated_normal_initializer(stddev=0.02),name='conv6')
        act6 = tf.nn.tanh(conv6, name='act6')
        return act6


def discriminator(input, is_train, reuse=False):
    with tf.variable_scope('dis') as scope:
        if reuse:
            scope.reuse_variables()
        # 32*32*64
        conv1 = tf.layers.conv2d(input, 64, kernel_size=[5, 5], strides=[2, 2], padding="SAME",kernel_initializer=tf.truncated_normal_initializer(stddev=0.02),name='conv1')
        bn1 = tf.contrib.layers.batch_norm(conv1, is_training = is_train, epsilon=1e-5, decay = 0.9,  updates_collections=None, scope = 'bn1')
        act1 = tf.nn.relu(conv1, name='act1')
		# 16*16*128
        conv2 = tf.layers.conv2d(act1, 128, kernel_size=[5, 5], strides=[2, 2], padding="SAME",kernel_initializer=tf.truncated_normal_initializer(stddev=0.02),name='conv2')
        bn2 = tf.contrib.layers.batch_norm(conv2, is_training=is_train, epsilon=1e-5, decay = 0.9,  updates_collections=None, scope='bn2')
        act2 = tf.nn.relu(bn2, name='act2')
		# 8*8*256
        conv3 = tf.layers.conv2d(act2, 256, kernel_size=[5, 5], strides=[2, 2], padding="SAME",kernel_initializer=tf.truncated_normal_initializer(stddev=0.02),name='conv3')
        bn3 = tf.contrib.layers.batch_norm(conv3, is_training=is_train, epsilon=1e-5, decay = 0.9,  updates_collections=None, scope='bn3')
        act3 = tf.nn.relu(bn3, name='act3')
		# 4*4*512
        conv4 = tf.layers.conv2d(act3, 512, kernel_size=[5, 5], strides=[2, 2], padding="SAME",kernel_initializer=tf.truncated_normal_initializer(stddev=0.02),name='conv4')
        bn4 = tf.contrib.layers.batch_norm(conv4, is_training=is_train, epsilon=1e-5, decay = 0.9,  updates_collections=None, scope='bn4')
        act4 = tf.nn.relu(bn4, name='act4')
        
        dim = int(np.prod(act4.get_shape()[1:]))
        fc1 = tf.reshape(act4, shape=[-1, dim], name='fc1')
         
        w2 = tf.get_variable('w2', shape=[fc1.shape[-1], 1], dtype=tf.float32,initializer=tf.truncated_normal_initializer(stddev=0.02))
        b2 = tf.get_variable('b2', shape=[1], dtype=tf.float32,initializer=tf.constant_initializer(0.0))
        return tf.add(tf.matmul(fc1, w2), b2, name='logits') 

def train():
    # INIT PART
    insertDimention = 100
    sampleMatrix = [5,5]
    discriminatorIters = 5	
    
    # Create variables to use later on
    with tf.variable_scope('input'):
        #real and fake image placholders
        real_image = tf.placeholder(tf.float32, shape = [None, height, width, depth], name='real_image')
        random_input = tf.placeholder(tf.float32, shape=[None, insertDimention], name='rand_input')
        is_train = tf.placeholder(tf.bool, name='is_train')
    
    fake_image = generator(random_input, insertDimention, is_train)
    real_result = discriminator(real_image, is_train)
    fake_result = discriminator(fake_image, is_train, reuse=True)
    
    # Optimize the discriminator.
    discriminatorLoss = tf.reduce_mean(fake_result) - tf.reduce_mean(real_result)  
    # Optimize the generator.
    generatorLoss = -tf.reduce_mean(fake_result)                               
            
	# Get all tensorflow variables
    trainingVariables = tf.trainable_variables()
	# Seperate those who belong to generator
    generatorVariables = [var for var in trainingVariables if 'gen' in var.name]
	# Seperate those who belong to discriminator
    discriminatorVariables = [var for var in trainingVariables if 'dis' in var.name]
    
    # Set the learning rate
    # https://arxiv.org/pdf/1603.05631.pdf learning rate should be 0.0002
	# Increase it by a little bit to make the learning faster
    learningRate = 0.0004
	# create the trainer with learning rate and found variables - Optional list or tuple of Variable objects to update to minimize loss
    trainerDiscriminator = tf.train.RMSPropOptimizer(learning_rate=learningRate).minimize(discriminatorLoss, var_list=discriminatorVariables)
    trainerGenerator = tf.train.RMSPropOptimizer(learning_rate=learningRate).minimize(generatorLoss, var_list=generatorVariables)
    
    # Clip the weights of the discriminator
    adjustedDiscriminatorWeights = [v.assign(tf.clip_by_value(v, -0.005, 0.005)) for v in discriminatorVariables]
    
    image_batch, samples_num = imagesToTensor()
    batch_num = int(samples_num / batchSize)

    # START NEW SESSION
    sess = tf.Session()
    # Create a new saver to save and restore variables.
    saver = tf.train.Saver()
    # Start all variables
    sess.run(tf.global_variables_initializer()) # tf.initializers.global_variables
    sess.run(tf.local_variables_initializer()) # tf.initializers.local_variables

    # Keep training on older models if any
    save_path = saver.save(sess, generatedPath)
    ckpt = tf.train.latest_checkpoint(generatedPath)
    print("latest checkpoint")
    print(ckpt)
    #return
    saver.restore(sess, save_path)
    coord = tf.train.Coordinator()
    threads = tf.train.start_queue_runners(sess=sess, coord=coord)

    # TRAINING PART
    print('Training discriminator on %d number of samples.' % samples_num)
    print('Batch size: %d. Batch num per epoch: %d, .Running %d Epochs' % (batchSize, batch_num, epoch))
    # Loop through all Epochs
    for i in range(epoch):
        print('Running Epoch ' + str(i))
        for j in range(batch_num):
            print('Running batch number ' + str(j))
            # Generate a random noise from -1 to 1 for 64 x 100
            train_noise = np.random.uniform(-0.5, 0.5, size=[batchSize, insertDimention]).astype(np.float32)
            for k in range(discriminatorIters):
                print(k)
                train_image = sess.run(image_batch)
                #wgan clip weights
                sess.run(adjustedDiscriminatorWeights)
                
                # Update the discriminator
                # Feed dict
                # the full image and label datasets are sliced to fit the batch_size for each step, 
                # matched with these placeholder ops, 
                # and then passed into the sess.run() function using the feed_dict parameter
                _, updatedDiscriminatorLoss = sess.run([trainerDiscriminator, discriminatorLoss],
                                    feed_dict={random_input: train_noise, real_image: train_image, is_train: True})

            # Update the generator
            _, updateGeneratorLoss = sess.run([trainerGenerator, generatorLoss],
                                feed_dict={random_input: train_noise, is_train: True})
            
        # On every 500 epoch, save the training model so we can start off where we ended
        if i%500 == 0:
            saver.save(sess, generatedPath + '/model' + str(i))  
        
        # On every 100 epoch, save a 5x5 sequence of images
        if i%100 == 0:
            # Create a random noise
            randomNoise = np.random.uniform(-0.5, 0.5, size=[batchSize, insertDimention]).astype(np.float32)
            generatedImage = sess.run(fake_image, feed_dict={random_input: randomNoise, is_train: False})
            
            print('Saving image ' + generatedPath + '/image' + str(i) + '.jpg')
            save_images(generatedImage, sampleMatrix ,generatedPath + '/image' + str(i) + '.jpg')
            
            print('Training epoch: [%d],Discriminator Loss:%f,Generator Loss:%f' % (i, updatedDiscriminatorLoss, updateGeneratorLoss))
    coord.request_stop()
    coord.join(threads)

def createFromCheckpoint():
	
    insertDimention = 100
    sampleMatrix = [5,5]
    with tf.variable_scope('input'):
        real_image = tf.placeholder(tf.float32, shape = [None, height, width, depth], name='real_image')
        random_input = tf.placeholder(tf.float32, shape=[None, insertDimention], name='rand_input')
        is_train = tf.placeholder(tf.bool, name='is_train')
    
    fake_image = generator(random_input, insertDimention, is_train)
    real_result = discriminator(real_image, is_train)
    fake_result = discriminator(fake_image, is_train, reuse=True)
    sess = tf.InteractiveSession()
    sess.run(tf.global_variables_initializer())
    variables_to_restore = slim.get_variables_to_restore(include=['gen'])
    print(variables_to_restore)
    saver = tf.train.Saver(variables_to_restore)
    ckpt = tf.train.latest_checkpoint(generatedPath) 
    print("latest checkpoint")
    print(ckpt)
    saver.restore(sess, ckpt)
	
    coord = tf.train.Coordinator()
    threads = tf.train.start_queue_runners(sess=sess, coord=coord)

	# Create a random noise
    randomNoise = np.random.uniform(-0.5, 0.5, size=[batchSize, insertDimention]).astype(np.float32)
    generatedImage = sess.run(fake_image, feed_dict={random_input: randomNoise, is_train: False})
    save_images(generatedImage, sampleMatrix ,generatedPath + '/image' + ckpt.split('/')[3] + '.jpg')
	
    coord.request_stop()
    coord.join(threads)	
	
if __name__ == "__main__":
    #train()
    createFromCheckpoint()

