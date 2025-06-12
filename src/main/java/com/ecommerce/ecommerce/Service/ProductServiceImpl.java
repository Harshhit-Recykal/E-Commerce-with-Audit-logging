//package com.ecommerce.ecommerce.Service;
//
//import com.ecommerce.ecommerce.DTO.ProductDto;
//import com.ecommerce.ecommerce.Entity.Product;
//import com.ecommerce.ecommerce.Repository.ProductRepository;
//import org.modelmapper.ModelMapper;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.stereotype.Service;
//
//import java.util.List;
//import java.util.Optional;
//import java.util.stream.Collectors;
//
//@Service
//public abstract class ProductServiceImpl implements ProductService {
//
//    @Autowired
//    private final ProductRepository productRepository;
//    private final ModelMapper modelMapper;
//
//    public ProductServiceImpl(ProductRepository productRepository, ModelMapper modelMapper) {
//        this.productRepository = productRepository;
//        this.modelMapper = modelMapper;
//    }
//
//@Override
//    public ProductDto createProduct(Product product){
//        this.productRepository.save(product);
//        return modelMapper.map(product,ProductDto.class);
//    }
//
//
//    public List<ProductDto> getAllProducts(){
//        List<Product> products = productRepository.findAll();
//         return products.stream().map(product->modelMapper.map(product,ProductDto.class)).collect(Collectors.toList());
//    }
//
//    @Override
//    public Optional<ProductDto> getProductById(Long id){
//        return productRepository.findById(id).map(product->modelMapper.map(product,ProductDto.class));
//    }
//
//
//    public Optional<ProductDto> getProductById(String name){
//        Optional<Product> prod = productRepository.findByName(name);
//        return prod.map(value->modelMapper.map(value,ProductDto.class));
//    }
//
//    public void UpdateProduct(Product product){
//        Long id  = product.getId();
//        if(id != null) {
//            productRepository.save(product);
//        }
//    }
//    @Override
//    public boolean deleteProduct(Long id) {
//        if (this.productRepository.existsById(id)) {
//            this.productRepository.deleteById(id);
//            return true;
//        }
//        return false;
//    }
//
//}
package com.ecommerce.ecommerce.Service;

import com.ecommerce.ecommerce.DTO.ProductDto;
import com.ecommerce.ecommerce.Entity.Product;
import com.ecommerce.ecommerce.Repository.ProductRepository;
import com.ecommerce.ecommerce.Service.ProductService;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final ModelMapper modelMapper;

    @Autowired
    public ProductServiceImpl(ProductRepository productRepository, ModelMapper modelMapper) {
        this.productRepository = productRepository;
        this.modelMapper = modelMapper;
    }

    @Override
    public ProductDto createProduct(Product product) {
        return modelMapper.map(productRepository.save(product), ProductDto.class);
    }

    @Override
    public Optional<ProductDto> getProductById(Long id) {
        return productRepository.findById(id)
                .map(product -> modelMapper.map(product, ProductDto.class));
    }

    @Override
    public Optional<ProductDto> getProductByName(String name) {
        return productRepository.findByName(name)
                .map(product -> modelMapper.map(product, ProductDto.class));
    }

    @Override
    public void updateProduct(Product product) {
        productRepository.save(product);
    }

    @Override
    public List<ProductDto> getAllProduct() {
        return productRepository.findAll().stream()
                .map(product -> modelMapper.map(product, ProductDto.class))
                .collect(Collectors.toList());
    }

    @Override
    public boolean deleteProduct(Long id) {
        if (productRepository.existsById(id)) {
            productRepository.deleteById(id);
            return true;
        }
        return false;
    }
}

