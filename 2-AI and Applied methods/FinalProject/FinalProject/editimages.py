import os, sys
from PIL import Image

from resizeimage import resizeimage

size = 256, 256

cwd = os.getcwd()
path = os.path.join(cwd, "data/")
dirs = os.listdir(path )

def resize():
    for item in dirs:
        if os.path.isfile(path+item): # item.endswith('.jpg')
            im = Image.open(path+item)
            f, e = os.path.splitext(path+item)
            imResize = im.resize((size), Image.ANTIALIAS)
            imResize.save( path + item, 'JPEG', quality=90)
            print("resized/image " + path + item + " saved")
        else: print("Failed " + path+item)
    print("image resize done")

    
    
def clearChannel():
    for item in dirs:
        png = Image.open(path+item)
        # print each
        if png.mode == 'RGBA':
            png.load() # required for png.split()
            background = Image.new("RGB", png.size, (0,0,0))
            background.paste(png, mask=png.split()[3]) # 3 is the alpha channel
            background.save(path+item, 'JPEG')
        else:
            png.convert('RGB')
            png.save(path+item, 'JPEG')
        print("Alpha channel cleared")
        
resize()
clearChannel()
print("Done")            
